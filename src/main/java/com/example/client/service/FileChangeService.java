package com.example.client.service;

import com.example.client.entity.ConfigData;
import com.example.client.entity.Content;
import com.example.client.utils.ConfigUtil;
import com.example.client.utils.NettyClientHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class FileChangeService {

    @Value("${config.file.path}") // 从配置文件中获取JSON文件的路径
    private String jsonFilePath;
    private WatchService watchService;
    private Map<WatchKey, Path> keyToPathMap;
    private ArrayList<Path> fileList;
    private final NettyClientService nettyClient;
    private ConfigData configData;

    //记录被修改的文件,只保留最新的修改记录,key为文件名,value为文件最后一次修改时间
    private Map<String, Map<String, String>> modifyMap;
    //记录被删除的文件,只保留最新的删除记录,key为文件名,value为文件删除时间
    private Map<String, String> deleteMap;
    private String userId;
    private static boolean isDown = false;
    private static final Object lock = new Object(); // 创建一个对象作为锁

    public FileChangeService(NettyClientService nettyClient) {
        this.nettyClient = nettyClient;
    }

    //初始化文件修改监听服务
    public void init(String userId) throws IOException, ParseException, InterruptedException {
        this.userId = userId;
        // 初始化 keyToPathMap
        keyToPathMap = new HashMap<>();
        // 初始化 fileList
        fileList = new ArrayList<>();
        // 初始化 WatchService
        watchService = FileSystems.getDefault().newWatchService();
        //初始化 modifyMap
        modifyMap = new HashMap<>();
        //初始化 deleteMap
        deleteMap = new HashMap<>();

        // 获取JSON文件的路径
        File configFile = new File(jsonFilePath);
        ObjectMapper objectMapper = new ObjectMapper();
        //将json文件转换成java实例
        configData = objectMapper.readValue(configFile, ConfigData.class);

        //配置文件中包含excel文件
        if (configData.getFileType().contains("excel")) {
            ArrayList<Content> content = configData.getExcel().getContent();
            for (Content c : content) {
                if (c.getUserId().equals(userId)) {
                    String filePath = c.getFilePath();
                    Path path = Paths.get(filePath);
                    //filePath下的文件是否存在
                    if (Files.exists(path)) {
                        //获取path路径下文件的修改时间
                        Date modifiedDate = getModifiedDate(path);
                        // 格式化修改时间
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
                        String lastModifiedDate = c.getModifiedDate();
                        Date date = sdf.parse(lastModifiedDate);//上次修改时间
                        int i = compareDate(date, modifiedDate);
                        //date比modifiedDate早,说明文件被修改了
                        if (i == 0) {
                            Map<String, String> map = new HashMap<>();
                            ObjectMapper mapper = new ObjectMapper();
                            map.put("filePath", c.getFilePath());
                            map.put("projectName", c.getProjectName());
                            map.put("modelName", c.getFileName());
                            map.put("uploadStatus", "等待上传");
                            modifyMap.put(String.valueOf(path), map);
                            System.out.println("initMap" + modifyMap);
//                            nettyClient.sendDataToElectron("localhost", 3000, mapper.writeValueAsString(modifyMap));
                            NettyClientService.sendDataToElectron("localhost", 3000, mapper.writeValueAsString(modifyMap));
                        } //date比modifiedDate晚,说明配置文件出错
                        else if (i == 1) {
                            System.out.println("配置文件出错了");
                            updateConfig(path);
                        }//时间相同,文件未修改
                        else {
                            System.out.println("文件未修改");
                        }

                        //文件存在,对该文件进行监听
                        registerFile(filePath);
                    } else {
                        System.out.println("文件丢失");
                    }
                }
            }

        }
        if (configData.getFileType().contains("txt")) {
            ArrayList<Content> content = configData.getTxt().getContent();
            for (Content c : content) {
                String filePath = c.getFilePath();
                if (Files.exists(Paths.get(filePath))) {
                    registerFile(filePath);
                } else {
                    System.out.println("文件丢失");
                    ;
                }
            }
        }

        //读取完配置文件后,对文件队列中的文件开始监听
        startWatching();
    }

    //将文件路径注册到需要监听的文件队列上
    private void registerFile(String filePath) throws IOException {
        Path file = Paths.get(filePath);
        WatchKey key = file.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
        fileList.add(file.getFileName());
        keyToPathMap.put(key, file.getParent());
    }

    //启动监听线程
    private void startWatching() {
        new Thread(() -> {
            while (true) {
                WatchKey key;
                try {
                    key = watchService.take(); // 阻塞等待事件
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                //获取监听的文件路径
                Path watchedPath = keyToPathMap.get(key);
                if (watchedPath != null) {
                    //pollEvents()方法会返回当前Key上面所有事件列表
                    for (WatchEvent<?> event : key.pollEvents()) {
                        //文件发生修改
                        if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Path modifiedFile = (Path) event.context();
                            if (fileList.contains(modifiedFile)) {
                                //处理修改的文件
                                Path fullPath = watchedPath.resolve(modifiedFile);//将watchedPath与modifiedFile路径拼接成完整路径
                                Map<String, String> map = new HashMap<>();
                                ObjectMapper mapper = new ObjectMapper();
                                // 设置序列化选项
                                mapper.enable(SerializationFeature.INDENT_OUTPUT);  // 设置缩进格式化输出
                                mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false); // 不序列化 null 值
                                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // 不将日期序列化为时间戳
                                File file = new File(fullPath.toString());
                                try {
                                    ConfigUtil.setPath(jsonFilePath);
                                    map.put("filePath", fullPath.toString());
                                    map.put("projectName", ConfigUtil.getContentByFilePath(fullPath.toString()).getProjectName());
                                    map.put("modelName", file.getName().substring(0, file.getName().lastIndexOf('.')));
                                    map.put("uploadStatus", "等待上传");
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                                try {
                                    modifyMap.put(fullPath.toString(), map);
                                    String s = mapper.writeValueAsString(modifyMap);
//                                    Thread.sleep(100);
//                                    Date modifiedDate = getModifiedDate(fullPath);
//                                    // 格式化修改时间
//                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
//                                    String lastModifiedDate = ConfigUtil.getContentByFilePath(fullPath.toString()).getModifiedDate();
//                                    Date date = sdf.parse(lastModifiedDate);//上次修改时间
//                                    int i = compareDate(date, modifiedDate);
//                                    System.out.println("i"+i);
                                    System.out.println(isDown);
                                    if(!isDown) {
                                        NettyClientService.sendDataToElectron("localhost", 3000, s);
                                    }
                                } catch (JsonProcessingException e) {
                                    throw new RuntimeException(e);
                                }  catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }//文件发生删除
                        else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                            // 文件被删除的处理逻辑
                            Path deletedFile = (Path) event.context();//被删除的文件名
                            if (fileList.contains(deletedFile)) {
                                Path fullPath = watchedPath.resolve(deletedFile);
                                if (!Files.exists(fullPath)) {
                                    // 处理删除的文件
                                    System.out.println("fullPath" + fullPath);
                                    System.out.println("File deleted: " + deletedFile);
                                    // 在这里执行删除文件的操作，通知 Electron 或其他处理
                                } else {
                                    // 文件存在，可能是被修改
                                }

                            }
                        }
                    }
                }
                key.reset(); // 重置 WatchKey，以便继续监听
            }
        }).start();
    }

    //修改配置文件中的文件修改时间
    public void updateConfig(Path fullPath) {
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                try {
                    //读取fullPath下的文件参数
                    BasicFileAttributes attributes = java.nio.file.Files.readAttributes(fullPath, BasicFileAttributes.class);
                    //获取文件修改的时间
                    FileTime fileTime = attributes.lastModifiedTime();

                    // 将FileTime转换为Date类型
                    Date modifiedDate = new Date(fileTime.toMillis());

                    // 格式化修改时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
                    String formattedDate = sdf.format(modifiedDate);
                    content.setModifiedDate(formattedDate);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String newJson = objectMapper.writeValueAsString(configData);
                    Files.write(Paths.get(jsonFilePath), newJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //更新模型描述
    public void updateDescription(Path fullPath, String description) {
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                try {
                    content.setDescription(description);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String newJson = objectMapper.writeValueAsString(configData);
                    Files.write(Paths.get(jsonFilePath), newJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateModelId(Path fullPath, String modelId) {
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                try {
                    content.setModelId(modelId);
                    ObjectMapper objectMapper = new ObjectMapper();
                    String newJson = objectMapper.writeValueAsString(configData);
                    Files.write(Paths.get(jsonFilePath), newJson.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //更新文件上传时间
    public void updateUploadDate(Path fullPath) throws IOException {
        ArrayList<Content> xlsxList = configData.getExcel().getContent();
        for (Content content : xlsxList) {
            if (content.getFilePath().equals(fullPath.toString())) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy年M月d日,HH:mm:ss");
                content.setUploadDate(sdf.format(new Date()));
                ObjectMapper objectMapper = new ObjectMapper();
                Files.write(Paths.get(jsonFilePath), objectMapper.writeValueAsString(configData).getBytes());
            }
        }
    }

    //获取path路径下文件的修改时间
    public Date getModifiedDate(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        //获取文件修改的时间
        FileTime fileTime = attributes.lastModifiedTime();
        // 将FileTime转换为Date类型
        return new Date(fileTime.toMillis());
    }

    //比较时间
    public int compareDate(Date date1, Date date2) {
        // 忽略毫秒部分，比较时间
        date1.setTime(date1.getTime() / 1000 * 1000); // 清除毫秒部分
        date2.setTime(date2.getTime() / 1000 * 1000);
        // 比较时间
        if (date1.before(date2)) {
            return 0;
        } else if (date1.after(date2)) {
            return 1;
        } else {
            return 2;
        }

    }

    public void removeNotify(String path) {
        System.out.println(modifyMap);
        //删除通知
        modifyMap.remove(path);
        System.out.println(modifyMap);
    }

    public static void setIsDown(boolean isDown) {
        synchronized (lock) {
            FileChangeService.isDown = isDown;
        }
    }
}
