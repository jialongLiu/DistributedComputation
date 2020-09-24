package real;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ServerProcess implements Runnable {
    private BufferedReader br;
    private Socket socket;
    private String root = "";
    private String workFile = "D:\\gitLab\\root";// 当前工作目录
    // private String workPath = "C:\\Users\\LLL\\Desktop";//当前工作目录
    private SocketAddress socketAddress;
    private PrintWriter pw;
    private UDPClient uc;

    ServerProcess(Socket inSocket, String inRoot, UDPClient inUc ,SocketAddress inSocketAddress) {
        socket = inSocket;
        workFile = inRoot;
        root = inRoot;
        uc = inUc;
        socketAddress = inSocketAddress;
    }

    @Override
    public void run() {
        // 创建发送消息变量
        BufferedWriter bw;
        try {
            bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // //装饰输出流，true,每写一行就刷新输出缓冲区，不用flush
            pw = new PrintWriter(bw);// 修改ture删掉，不用自动flush
        } catch (IOException e) {
            e.printStackTrace();
        }


        // 发送连接成功消息
        String successMsg = socket.getInetAddress() + ":" + socket.getPort() + ">连接成功";
        System.out.println(successMsg);
        try {
            outputTCP(successMsg);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        // 循环等待获取客户端发送的数据
        while (true) {
            // 获取客户端输入字符流
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                // 用UDP发送命令所触发的发送信息
                String bre;
                bre = cmdProcess(uc);
                if(bre.equals("break"))break;
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
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
            //绝对路径存在
                workFile = path;//BUG修复：如果输入无效路径，工作目录不变
                output = workFile+" > "+"OK";
            }else if(tips.equals("the dir is not exist!")){
            //绝对路径不存在
                output = "unknown dir!";
            }else {
            //相对路径问题
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

