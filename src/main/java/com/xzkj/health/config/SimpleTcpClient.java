package com.xzkj.health.config;

import java.io.*;
import java.net.Socket;

/**
 * 简单的TCP客户端测试工具
 * 用于测试Netty服务器连接
 */
public class SimpleTcpClient {
    
    public static void main(String[] args) {
//        String host = "10.8.138.214";
        String host = "192.168.1.3";
        int port = 9000;
        
        System.out.println("尝试连接到 " + host + ":" + port);
        
        try (Socket socket = new Socket(host, port)) {
            System.out.println("✓ 连接成功！");
            System.out.println("本地地址: " + socket.getLocalSocketAddress());
            System.out.println("远程地址: " + socket.getRemoteSocketAddress());
            
            // 发送测试数据
            OutputStream out = socket.getOutputStream();
            String testData = "IW*001*TEST#";
            out.write(testData.getBytes());
            out.flush();
            System.out.println("✓ 发送数据: " + testData);
            
            // 等待响应
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            socket.setSoTimeout(3000); // 3秒超时
            
            try {
                int bytesRead = in.read(buffer);
                if (bytesRead > 0) {
                    String response = new String(buffer, 0, bytesRead);
                    System.out.println("✓ 收到响应: " + response);
                } else {
                    System.out.println("- 没有收到响应数据");
                }
            } catch (Exception e) {
                System.out.println("- 读取响应超时（正常，服务器可能不响应）");
            }
            
            Thread.sleep(1000); // 保持连接1秒
            System.out.println("✓ 测试完成");
            
        } catch (IOException e) {
            System.err.println("✗ 连接失败: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
