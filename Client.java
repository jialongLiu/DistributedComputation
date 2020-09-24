package real;

import java.io.*;
import java.net.*;
import java.util.Scanner;





public class Client {
    public static void main(String[] args) throws UnknownHostException, IOException {

        // 测试ls命令
        FileClient fc = new FileClient();
        fc.client();
        fc.sendCmd();

    }
}

class FileClient{
    Socket socket;
    static final int cport = 2021;//此处修改端口号
    static final String host = "127.0.0.1";//此处修改链接地址
    BufferedWriter bw;

    // 连接IP为host，端口为cport的服务器
    public void client()throws UnknownHostException, IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, cport));
        tcpConnectListen();
    }
    
    // TCP发送命令消息
    public void sendCmd(){
        try {
            //读写信息的存储变量初始化和装饰
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//要发送的字符信息
            PrintWriter pw = new PrintWriter(bw,true);//装饰输出流，及时刷新

            // 输入命令
            Scanner in = new Scanner(System.in); //接受用户从控制台输入的信息

            // 初始化UDP监听,初始化必须指定udp端口号
            UdpServer us = new UdpServer(2020);
            
            //从控制台获取信息并且发送给服务器
            String msg = null;
            while ((msg = in.next()) != null) {
                //发送给服务器端
                if(msg.equals("get")){
                    pw.println(msg);//先发送get消息
                    if((msg = in.next()) != null){
                        String fileName =msg;//要保存的文件名
                        pw.println(fileName);//发送文件名
                        us.getFile(fileName);//紧接着立刻通过UDPserver获取UDP客户端发来的文件内容信息
                    };
                }else if(msg.equals("bye")) {
                    pw.println(msg); 
                    break; //退出
                }else{
                    // 普通命令直接发送并监听UDP
                    pw.println(msg); 
                    tcpServerListen();
                    // us.udpServiceListen();//  UDP接收服务器发来的信息 //修改为TCP交互
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != socket) {
                try {
                    socket.close(); //断开连接
                } catch (IOException e) {
                e.printStackTrace();
                }
            }
        }
    }

    //TCP监听连接消息
    public void tcpConnectListen() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String msg = br.readLine();
        System.out.println(msg);
    }

    //TCP服务器listen
    public void tcpServerListen() throws IOException {
        while (true) {
            //客户端输入流，接收服务器消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
               String msg = br.readLine();
            // System.out.println(msg); //输出服务器返回的消息
            
            // 获取并输出数据包中客户端信息
            if(msg.equals("end")){
                break;//额外终止信息辅助跳出循环
            }else if(msg.equals("next")){
                break;//中间操作，继续读取控制台信息，也就是空格后面的信息
            }else{
                System.out.println(msg);
            }

		}
    }
}

//UDP服务器端
class UdpServer {
	int port = 2020;
	DatagramSocket socket;

    // 初始化并启动监听port端口
	public UdpServer(int udpPort) throws SocketException {
        port = udpPort;
	}

    // 循环接收port端口发来的信息
	public void udpServiceListen() throws IOException {
		socket = new DatagramSocket(port); // 服务端DatagramSocket
		while (true) {
			DatagramPacket dp = new DatagramPacket(new byte[512], 512);//初始化数据包大小为512
            socket.receive(dp); // 接收客户端信息并放到数据包里
			String msg = new String(dp.getData(), 0, dp.getLength());
            
            // 获取并输出数据包中客户端信息
            if(msg.equals("end")){
                break;//额外终止信息辅助跳出循环
            }else if(msg.equals("next")){
                break;//中间操作，继续读取控制台信息，也就是空格后面的信息
            }else{
                System.out.println(dp.getAddress() + ":" + dp.getPort() + ">" + msg);
            }
        }
        socket.close();
	}
    
    // 通过udp获取文件内容并返回字符串
    public void getFile(String name) throws IOException {
		socket = new DatagramSocket(port); // 服务端DatagramSocket
        while (true) {
            // 创建接收数据的udp包
            DatagramPacket dp = new DatagramPacket(new byte[512], 512);//初始化数据包大小为512
            
            // 读取头部信息判断读文件开始标志
            socket.receive(dp); // 接收客户端信息并放到数据包里
            String msg = new String(dp.getData(), 0, dp.getLength());
            if(msg.equals("begin file!")){
                // 创建文件
                FileOutputStream fileOutput = new FileOutputStream(name);
                
                // 循环读udp包并且写文件
                while(true){
                    socket.receive(dp);

                    // 判断结束符,跳出循环
                    String dpStr = new String(dp.getData(), 0, dp.getLength());
                    if(dpStr.equals("end file!"))break;//如果文件发送完毕，尾部信息获取并跳出循环。

                    //写入文件
                    fileOutput.write(dp.getData(),0,dp.getLength());
                    // fileOutput.write(msg.getBytes());//不需要转换为string进行发送，会出现乱码
                    fileOutput.flush();
                }
                fileOutput.close();
                System.out.println("file is ok!");//文件发送完毕
                break;
            }else if(msg.equals("file is not exists!")){
                System.out.println("unknown file");
                break;
            }else{
                System.out.println("it is a directory!");
                break;
            }
        }
        socket.close();
    }


}
