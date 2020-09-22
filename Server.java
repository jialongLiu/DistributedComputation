
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;


// 实现了循环监听
// 利用类属性进行提示，并且转换当前目录，实现全局转换工作目录
// 文件分割后，分别发送，最后一个udp包不够512进行判断，如果发送完最后一个包，额外发送尾部信息告知发送完毕。
public class Server{
    public static void main(String[] args) throws IOException, InterruptedException {

        // 测试ls命令
        FileServer fs = new FileServer();
        fs.server();
    }
}

class FileServer{
    private BufferedReader br;
    private ServerSocket serverSocket;
    private Socket socket;
    private final int sport = 2021;
    private final int maxConnect = 2;
    private String workFile = "C:\\Users\\LLL\\Desktop";//当前工作目录
    // private String workPath = "C:\\Users\\LLL\\Desktop";//当前工作目录
    private SocketAddress socketAddress ;
     
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
                workFile = "C:\\Users\\LLL\\Desktop";//每一次新的连接都转换当前工作目录为根目录
                System.out.println(socket.getInetAddress()+":"+socket.getPort()+">连接成功");
                // 循环等待获取客户端发送的数据
                while(true){

                    // 获取客户端输入字符流
                    br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // 用UDP发送命令所触发的发送信息
                    String bre = cmdProcess(uc);
                    if(bre.equals("break"))break;

                    // //给客户端发送you said信息
                    // outputTCP(br);

                }
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

    //给客户端发送you said信息
    public void outputTCP(BufferedReader br) throws IOException {
        // 要给客户端发送的信息（you said）
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        // //装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
        PrintWriter pw = new PrintWriter(bw,true);
        String info = null; //接收用户输入的信息
        while ((info = br.readLine()) != null) {
            System.out.println(info); //输出用户发送的消息
            pw.println("you said:" + info); //向客户端返回用户发送的消息，println输出完后会自动刷新缓冲区
            if (info.equals("bay")) { //如果用户输入“bye”就退出
                break;
            }
        }
    }

    // 列出目标目录下所有文件返回一个string[]
    public String[] lsGetdir(String path) {
        // 获取当前工作目录
        File f = new File(path);
        File workplace = new File(f.getAbsolutePath());
        return workplace.list();
    }

    //lsProcess()ls命令处理
    public void lsProcess(UDPClient uc) throws IOException {
        // 获取目录下所有文件名存到string[]
        String[] filesName = lsGetdir(workFile);
        // 判断目录里是不是不存在文件
        if(filesName == null){
            uc.sendStr("this dir have not things!", socketAddress);
        }else{
            // 发送目录下所有文件名（ls）
            for(int i =0; i < filesName.length;i++){
                uc.sendStr(filesName[i],socketAddress);
            }
        }
    }
    
    //cdProcess()cd命令处理
    public void cdProcess(UDPClient uc) throws IOException {
        uc.sendStr("next",socketAddress);//额外终止信息辅助跳出循环
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String path = br.readLine();
        if(path.equals("..")){
            File tempFile = new File(workFile);
            String parentPath = tempFile.getParent();
            path = parentPath;
        }else if(path.equals(".")){
            path = "C:\\Users\\LLL\\Desktop";
        }

        //cd命令判断目录是否存在并给出提示
        String tips = cdJudge(path);
        String output = "path:"+path+"\n"+"tips:"+tips;
        if(tips.equals("the dir is exist!"))workFile = path;//BUG修复：如果输入无效路径，工作目录不变
        System.out.println(output);   
        uc.sendStr(output, socketAddress); 
    }
    
    //getProcess()get命令处理
    public void getProcess(UDPClient uc) throws IOException, InterruptedException {
        //获取文件名
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String getFileName = br.readLine();
        //循环获取文件并发送
        File getFile = new File(workFile+"\\"+getFileName);
        // 判断文件是否存在
        if(!getFile.exists()){
            // 如果文件不存在发送消息
            uc.sendStr("file is not exists!", socketAddress);
        }else{
            // 文件存在打开文件并且传输
            byte[] getFileByte = new byte[512];//限制每个udp包传送512
            FileInputStream fileInput = new FileInputStream(getFile);
            uc.sendStr("begin file!", socketAddress);//发送头部信息
            int sizeOfByte=0;
            //循环传送文件
            while((sizeOfByte=fileInput.read(getFileByte)) != -1){
                // String ss = new String(getFileByte);//测试转换为string出现乱码
                // System.out.println( ss);//测试删掉
                if(sizeOfByte !=512){
                    byte[] lastByteArray = Arrays.copyOf(getFileByte, sizeOfByte);
                    uc.sendByteArray(lastByteArray, socketAddress);
                }else{//byte[]小于512，说明是最后一个包
                    uc.sendByteArray(getFileByte,socketAddress);
                    TimeUnit.MICROSECONDS.sleep(1);
                    getFileByte = new byte[512];//每次发送完byte[]要初始化
                }
                
            }
            fileInput.close();
            uc.sendStr("end file!", socketAddress);
        }
    }
    
    //cd命令判断目录是否存在并给出提示
    public String cdJudge(String nowPath){
        File newf = new File(nowPath);
        if(newf.exists()){
            return "the dir is exist!";
        }else{
            return "the dir is not exist!";
        }

    };

    // 发送ls命令的结果
    public String cmdProcess(UDPClient uc) throws IOException, InterruptedException {
        
        String brStr =br.readLine();
        System.out.println(brStr);

        if(brStr.equals("ls")){
        // ls命令处理
            lsProcess(uc);
        }else if(brStr.equals("cd")){
        //cd命令处理
            cdProcess(uc);  
        }else if(brStr.equals("bay")){
        //bay命令处理     
            uc.sendStr("end",socketAddress);//额外终止信息辅助跳出循环
            return "break";//实现了循环监听
        }else if(brStr.equals("get")){
        //get命令处理
            getProcess(uc);
            return "nothing";
        }else{
        //无效命令处理
            uc.sendStr("invalid command!", socketAddress);
        }
        uc.sendStr("end",socketAddress);//额外终止信息辅助跳出循环
        return "nothing";
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
