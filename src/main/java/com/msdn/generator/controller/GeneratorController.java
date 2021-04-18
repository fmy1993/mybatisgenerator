package com.msdn.generator.controller;

import com.msdn.generator.entity.Config;
import com.msdn.generator.entity.GenerateParameter;
import com.msdn.generator.service.GenerateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
public class GeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(GeneratorController.class);
    @Autowired
    private GenerateService generateService;

    /*
        // 请求参数
        {
            "database": "db_fl_sale",
            "flat": true,
            "group": "business",
            "host": "47.103.92.197",
            "module": "sale",
            "password": "ZhdJk2020@pwd",
            "port": 3306,
            "table": [
                "t_xs_sale_credit_bill"
            ],
            "username": "zhd"
        }
     */
    @PostMapping("/generator/build")
    public void build(@RequestBody GenerateParameter parameter, HttpServletResponse response) throws Exception {
        logger.info("**********欢迎使用基于FreeMarker的模板文件生成器**********");
        logger.info("************************************************************");
        String uuid = UUID.randomUUID().toString();
        for (String table : parameter.getTable()) {
            generateService.generate(table, parameter, uuid);
        }
        logger.info("**********模板文件生成完毕，准备下载**********");
        String path = Config.OutputPath + File.separator + uuid;
        //设置响应头控制浏览器的行为，这里我们下载zip
        response.setHeader("Content-disposition", "attachment; filename=code.zip");
        response.setHeader("Access-Control-Expose-Headers", "Content-disposition");
        // 将response中的输出流中的文件压缩成zip形式
        ZipDirectory(path, response.getOutputStream());
        // 递归删除目录
        FileSystemUtils.deleteRecursively(new File(path));
        logger.info("************************************************************");
        logger.info("**********模板文件下载完毕，谢谢使用**********");
    }

    /**
     * 一次性压缩多个文件，文件存放至一个文件夹中
     */
    public static void ZipDirectory(String directoryPath, ServletOutputStream outputStream) {
        InputStream input = null;
        ZipOutputStream output = null;
        try {
            output = new ZipOutputStream(outputStream);
            List<File> files = getFiles(new File(directoryPath));
            for (File file : files) {
                input = new FileInputStream(file);
                output.putNextEntry(new ZipEntry(file.getPath().substring(directoryPath.length() + 1)));
                int temp = 0;
                while ((temp = input.read()) != -1) {
                    output.write(temp);
                }
                input.close();
            }
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<File> getFiles(File file) {
        List<File> files = new ArrayList<>();
        for (File subFile : file.listFiles()) {
            if (subFile.isDirectory()) {
                List<File> subFiles = getFiles(subFile);
                files.addAll(subFiles);
            } else {
                files.add(subFile);
            }
        }
        return files;
    }
}
