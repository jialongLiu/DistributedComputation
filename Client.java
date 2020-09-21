import java.io.*;
import java.net.*;
import java.util.Scanner;

import javax.print.DocFlavor.STRING;


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
    }
    
    // TCP发送命令消息
    public void sendCmd(){
        try {
            //读写信息的存储变量初始化和装饰
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));//要发送的字符信息
            PrintWriter pw = new PrintWriter(bw,true);//装饰输出流，及时刷新

            // 输入命令
            Scanner in = new Scanner(System.in,"GBK"); //接受用户从控制台输入的信息

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
                }else if(msg.equals("bay")) {
                    break; //退出
                }else{
                    // 普通命令直接发送并监听UDP
                    pw.println(msg); 
                    us.service();//  UDP接收服务器发来的信息
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
}

//UDP服务器端
class UdpServer {
	int port = 2020;
	DatagramSocket socket;

    // 初始化并启动监听port端口
	public UdpServer(int udpPort) throws SocketException {
        port = udpPort;
		socket = new DatagramSocket(port); // 服务端DatagramSocket
        System.out.println("UDP服务器初始化。");
	}

    // 循环接收port端口发来的信息
	public void service() throws IOException {
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
	}
    
    // 通过udp获取文件内容并返回字符串
    public void getFile(String name) throws IOException {
        // 创建文件
        FileOutputStream fileOutput = new FileOutputStream(name);
        while (true) {
			DatagramPacket dp = new DatagramPacket(new byte[512], 512);//初始化数据包大小为512
            socket.receive(dp); // 接收客户端信息并放到数据包里
            // 判断结束符
            String msg = new String(dp.getData(), 0, dp.getLength());
            if(msg.equals("file is end!"))break;
            fileOutput.write(msg.getBytes());
            fileOutput.flush();
            
            // 获取并输出数据包中客户端信息
            System.out.println(dp.getAddress() + ":" + dp.getPort() + ">" + msg);

        }
        fileOutput.close();
    }
}
