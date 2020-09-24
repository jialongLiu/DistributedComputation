package real;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 实现了循环监听
// 利用类属性进行提示，并且转换当前目录，实现全局转换工作目录
// 文件分割后，分别发送，最后一个udp包不够512进行判断，如果发送完最后一个包，额外发送尾部信息告知发送完毕。
// 为了更好的鲁棒性，如果目录为真正根目录，也会提醒。如C盘
//兼容两种cd命令， 可以绝对路径，也可以相对路径。

public class Server {

    static ExecutorService executorService; // 线程池
    final static int POOLSIZE = 10; // 单个处理器线程池同时工作线程数目

    public static void main(String[] args) throws IOException, InterruptedException {

        // 服务器启动时需要传递root参数
        System.out.println("please input root :");
        Scanner sc = new Scanner(System.in);
        String workpalce = sc.nextLine();
        FileServer fs =new FileServer(workpalce);
        fs.server();
        sc.close();

        // // 多线程
        // executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * POOLSIZE);// 创建线程池
        // while(true){
        //     executorService.execute(new FileServer(workpalce));
        // }
    }
}

// //多线程类
// class MultiThreading{
// Private
// }

class FileServer {
    ExecutorService executorService;
    private ServerSocket serverSocket;
    private Socket socket;
    private final int sport = 2021;
    private final int maxConnect = 20;
    private String root = "";
    private String workFile = "D:\\gitLab\\root";// 当前工作目录
    private String errMsg = "";
    private SocketAddress socketAddress;

    FileServer(String workplace) {
        File workfile = new File(workplace);
        if (workfile.exists() && workfile.isDirectory()) {
            root = workplace;// workfile经常变，但是root变量不变，所以每一次新的连接可以直接用root
            workFile = workplace;
            System.out.println("the input root is " + workplace);
        } else {
            root = "D:\\gitLab\\root";
            errMsg = "the input root is not exist! so the root is D:\\gitLab\\root";
            System.out.println(errMsg);
        }
        executorService = Executors.newFixedThreadPool(10);
    }

    // TPC服务器启动
    public void server() throws InterruptedException {
        try{
            serverSocket = new ServerSocket(sport, maxConnect);
            System.out.println("TCP服务器启动");
            
            // 启动UDP通信客户端
            UDPClient uc = new UDPClient(2020,"127.0.0.1");//初始化必须指定端口含和IP
            socketAddress = uc.pointUdp();

            // 循环等待TCP建立连接
            while(true){
                socket = serverSocket.accept();//等待客户机与服务器链接
                workFile = root;//每一次新的连接都转换当前工作目录为根目录，workfile经常变，但是root变量不变，所以每一次新的连接可以直接用root
                
                //起线程
                executorService.execute(new ServerProcess(socket,root,uc,socketAddress));
            }
            
        }catch( IOException  e){
            e.printStackTrace();
        }finally{
            if(null != socket){
                try {
                    socket.close(); //断开连接
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


//UDP客户端
class UDPClient {
	int remotePort = 2020; // 服务器端口
    String remoteIp = "127.0.0.1"; // 服务器IP
	DatagramSocket socket; // 客户端DatagramSocket

    // 初始化UDP随机可用端口
	public  UDPClient(int Port,String Ip) throws SocketException {
        socket = new DatagramSocket(); // 随机可用端口，又称匿名端口
        remotePort = Port;
        remoteIp = Ip;
    }
    
    // 指向UDP的IP和port
    public SocketAddress pointUdp() {
        // 通过服务器端ip和端口号进行发消息，因为udp不需要建立连接
        SocketAddress socketAddress = new InetSocketAddress(remoteIp, remotePort);
        return socketAddress ;
    }

    // 将string类型信息用UDP发送给remoteip和remoteport的远端
	public void sendStr(String str,SocketAddress socketAddress ) throws IOException {
        String s = str; // 通过参数获取输入
        byte[] info = s.getBytes();
        // 创建数据包，指定服务器地址
        DatagramPacket dp = new DatagramPacket(info, info.length,
                socketAddress);
        socket.send(dp); // 向服务器端发送数据包
    }

    // 将byte[]类型信息用UDP发送给remoteip和remoteport的远端
	public void sendByteArray(byte[] byteArray,SocketAddress socketAddress ) throws IOException {
        byte[] info = byteArray;// 通过参数获取输入
        // 创建数据包，指定服务器地址
        DatagramPacket dp = new DatagramPacket(info, info.length,
                socketAddress);
        socket.send(dp); // 向服务器端发送数据包
	}

}
