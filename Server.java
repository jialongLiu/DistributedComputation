
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;



// 实现了循环监听
// 利用类属性进行提示，并且转换当前目录，实现全局转换工作目录
// 文件分割后，分别发送，最后一个udp包不够512进行判断，如果发送完最后一个包，额外发送尾部信息告知发送完毕。
// 为了更好的鲁棒性，如果目录为真正根目录，也会提醒。如C盘
//兼容两种cd命令， 可以绝对路径，也可以相对路径。

public class Server{
    public static void main(String[] args) throws IOException, InterruptedException {

        //服务器启动时需要传递root参数
        System.out.println("please input root :");
        Scanner sc = new Scanner(System.in);
        String workpalce = sc.nextLine();
        FileServer fs = new FileServer(workpalce);// 测试ls命令
        fs.server();
        sc.close();

        // // 测试入口（免输入root）
        // FileServer fs = new FileServer("C:\\Users\\LLL\\Desktop");
        // fs.server();
    }
}

class FileServer{
    private BufferedReader br;
    private ServerSocket serverSocket;
    private Socket socket;
    private final int sport = 2021;
    private final int maxConnect = 2;
    private String root = "";
    private String workFile = "D:\\gitLab\\root";//当前工作目录
    // private String workPath = "C:\\Users\\LLL\\Desktop";//当前工作目录
    private SocketAddress socketAddress ;
    private String errMsg = "";
    private PrintWriter pw;
    
     
    FileServer(String workplace) {
        File workfile = new File(workplace);
        if(workfile.exists() && workfile.isDirectory()){
            root = workplace;//workfile经常变，但是root变量不变，所以每一次新的连接可以直接用root
            workFile = workplace;
            System.out.println("the input root is "+workplace);
        }else{
            root = "D:\\gitLab\\root";
            errMsg = "the input root is not exist! so the root is D:\\gitLab\\root";
            System.out.println(errMsg);
        }
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
                
                // 创建发送消息变量
                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                // //装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
                pw = new PrintWriter(bw);//修改ture删掉，不用自动flush

                //发送连接成功消息
                String successMsg = socket.getInetAddress()+":"+socket.getPort()+">连接成功";
                System.out.println(successMsg);
                outputTCP(successMsg);
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
    public void outputTCP(String succeString) throws IOException, InterruptedException {
        
        String info = succeString; //发送tcp连接成功消息
        pw.println(info); //向客户端返回用户发送的消息，println输出完后会自动刷新缓冲区
        pw.flush();
        Thread.sleep(30);//重要！解决连续发送TCP只接受到一个信息的问题。
    }

    // 列出目标目录下所有文件返回一个string[]
    public String[] lsGetdir(String path) {
        // 获取当前工作目录
        File f = new File(path);
        File workplace = new File(f.getAbsolutePath());
        return workplace.list();
    }

    //将ls发送子文件信息进行封装
    public String packageFileDir(String fn) {
        File tf = new File(fn);
        String res ="";
        if(tf.isDirectory()){
            res = "<dir>"+"     "+tf.getName();
        }else if(tf.isFile()){
            res = "<file>"+"    "+tf.getName();
        }
        for(int i = 24-tf.getName().length(); i>0;i-- ){
            res = res +" ";
        }
        return res+tf.length()+"B";
    }

    //lsProcess()ls命令处理
    public void lsProcess() throws IOException, InterruptedException {
        // 获取目录下所有文件名存到string[]
        String[] filesName = lsGetdir(workFile);
        // 判断目录里是不是不存在文件
        if(filesName == null){
            outputTCP("this dir have not things!");
            // uc.sendStr("this dir have not things!", socketAddress);//修改为TCP交互
        }else{
            // 发送目录下所有文件名（ls）
            for(int i =0; i < filesName.length;i++){
                outputTCP(packageFileDir(workFile+"\\"+ filesName[i]));
                // uc.sendStr(filesName[i],socketAddress);//修改为TCP交互
            }
        }
    }
    
    //cdProcess()cd命令处理
    public void cdProcess() throws IOException, InterruptedException {
        outputTCP("next");
        // uc.sendStr("next",socketAddress);//额外终止信息辅助跳出循环//修改为TCP完成交互
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String path = br.readLine();
        System.out.println(path);//ljl
        if(path.equals("..")){
        // 上级目录
            // 如果在根目录，则cd ..不进行操作
            if(workFile.equals("D:\\gitLab\\root")){
                path = workFile;
                outputTCP("the dir is root,path : D:\\gitLab\\root");
                // uc.sendStr("the dir is root,path : C:\\Users\\LLL\\Desktop", socketAddress);//修改为TCP完成交互
            }else if(workFile.equals("D:\\")|| workFile.equals("d:\\")){
            // 如果当前目录为c盘，上一级没有，就回去
                path = root;
                workFile =root;
                outputTCP("this dir is null!, so path : D:\\gitLab\\root");
            }else{
                File tempFile = new File(workFile);
                String parentPath = tempFile.getParent();
                path = parentPath;

                //cd命令判断目录是否存在并给出提示
                String tips = cdJudge(path);
                String output = "path:"+path+"\n"+"tips:"+tips;
                if(tips.equals("the dir is exist!"))workFile = path;//BUG修复：如果输入无效路径，工作目录不变
                System.out.println(output);   
                outputTCP(output);
                // uc.sendStr(output, socketAddress); //修改为TCP完成交互
            }
        }else if(path.equals(".")){
        //当前目录
            workFile = "D:\\gitLab\\root";
            outputTCP("path : D:\\gitLab\\root");
            // uc.sendStr("path : C:\\Users\\LLL\\Desktop", socketAddress);//修改为TCP完成交互
        }else{
        //普通目录
            //cd命令判断目录是否存在并给出提示
            String tips = cdJudge(path);
            String output = "path:"+path+"\n"+"tips:"+tips;
            if(tips.equals("the dir is exist!")){
                workFile = path;//BUG修复：如果输入无效路径，工作目录不变
                output = workFile+" > "+"OK";
            }else if(tips.equals("the dir is not exist!")){
                output = "unknown dir!";
            }else {
                workFile = cdJudge(path);
                output = workFile +" > "+"OK";
            }
            System.out.println(output);   
            outputTCP(output);
            // uc.sendStr(output, socketAddress); //修改为TCP完成交互
        }
    }
    
    //getProcess()get命令处理
    public void getProcess(UDPClient uc) throws IOException, InterruptedException {
        //获取文件名
        br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String getFileName = br.readLine();
        //循环获取文件并发送
        File getFile = new File(workFile+"\\"+getFileName);
        Thread.sleep(100);//保证在发消息的时候，客户端已经准备好了接收。
        // 判断文件是否存在
        if(!getFile.exists()){
            // 如果文件不存在发送消息
            uc.sendStr("file is not exists!", socketAddress);
        }else if(getFile.isDirectory()){
            uc.sendStr("it is a directory!", socketAddress);
        }else{
            // 文件存在打开文件并且传输
            byte[] getFileByte = new byte[512];//限制每个udp包传送512
            BufferedInputStream fileInput = new BufferedInputStream(new FileInputStream(getFile));//务必要用bufferreader，否则文件最后一行乱码。
            // FileInputStream fileInput = new FileInputStream(getFile);
            uc.sendStr("begin file!", socketAddress);//发送头部信息
            int sizeOfByte=0;
            //循环传送文件
            while((sizeOfByte=fileInput.read(getFileByte)) != -1){
                // String ss = new String(getFileByte);//测试转换为string出现乱码
                // System.out.println( ss);//测试删掉
                if(sizeOfByte !=512){
                    byte[] lastByteArray = new byte[sizeOfByte];
                    lastByteArray = Arrays.copyOf(getFileByte, sizeOfByte);
                    uc.sendByteArray(lastByteArray, socketAddress);
                    Thread.sleep(100);
                    String ss = new String(lastByteArray,0,lastByteArray.length);
                    System.out.println(ss);
                    getFileByte = new byte[512];//每次发送完byte[]要初始化
                }else{//byte[]小于512，说明是最后一个包
                    uc.sendByteArray(getFileByte,socketAddress);
                    TimeUnit.MICROSECONDS.sleep(10);
                    getFileByte = new byte[512];//每次发送完byte[]要初始化
                }
                
                
            }
            fileInput.close();
            Thread.sleep(100);//等待客户端开始接收消息。
            uc.sendStr("end file!", socketAddress);
        }
    }
    
    //cd命令判断目录是否存在并给出提示
    public String cdJudge(String nowPath){
        File newf = new File(nowPath);
        if(newf.exists()){
            return "the dir is exist!";
        }else if(cdJudgeNow(nowPath).equals("is dic, not in!")|| cdJudgeNow(nowPath).equals("unknow dic")){
            return "the dir is not exist!";
        }else{
            return cdJudgeNow(nowPath);
        }

    };

    //cd判断当前目录是否存在该文件夹
    public String cdJudgeNow(String filename) {
        File rootFile = new File(workFile);
        File[] fileList = rootFile.listFiles();
        for (int i = 0; i < fileList.length; i++){
			if (fileList[i].getName().equals(filename)){//找到了同名的文件夹或文件
				if (fileList[i].isDirectory()){//名字对应文件夹
				    return workFile+"\\"+filename;
				}else{// 名字对应文件
					return "is dic, not in!";
				}
			}
		}
        return "unknow dic";
    }

    // 发送ls命令的结果
    public String cmdProcess(UDPClient uc) throws IOException, InterruptedException {
        
        String brStr =br.readLine();
        System.out.println(brStr);

        if(brStr.equals("ls")){
        // ls命令处理
            lsProcess();
        }else if(brStr.equals("cd")){
        //cd命令处理
            cdProcess();  
        }else if(brStr.equals("bye")){
        //bye命令处理     
            outputTCP("end");
            // uc.sendStr("end",socketAddress);//额外终止信息辅助跳出循环//修改为TCP完成交互
            return "break";//实现了循环监听
        }else if(brStr.equals("get")){
        //get命令处理
            getProcess(uc);
            return "nothing";
        }else{
        //无效命令处理
            outputTCP("unknown cmd!");
            // uc.sendStr("unknown cmd!", socketAddress);//修改为TCP完成交互
        }
        outputTCP("end");
        // uc.sendStr("end",socketAddress);//额外终止信息辅助跳出循环//修改为TCP交互
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
