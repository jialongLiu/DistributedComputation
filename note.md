## 一、Java知识
### 1. java错误发现
1. tostring()并不能把bufferreader对象转换成相应的string，而是返回一个string类型，但不是br的内容。要用readline()
```
源码如下
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode());
    }
```

2.  br.readLine(); 方法会卡住，一直等待br有内容才可以输出。所以程序执行到这个，如果br是空就一直在等结果。
```
                //输出服务器返回的消息
                System.out.println(br.readLine()); 
```
3. UDP传输文件的时候，千万别转换成string，直接用byte[]写文件即可，这样不会有乱码出现
```
            String msg = new String(dp.getData(), 0, dp.getLength());
            if(msg.equals("file is end!"))break;
            fileOutput.write(dp.getData());
            // fileOutput.write(msg.getBytes());//不需要转换为string进行发送，会出现乱码
            fileOutput.flush();
```
4. 解决连续发送TCP只接受到一个信息的问题。TCP用BufferedReader接受数据速度跟不上TCP发送的速度，所以手动flush+线程睡眠，能够让tcp按顺序发送
```
//给客户端发送you said信息
    public void outputTCP(String succeString) throws IOException, InterruptedException {
        
        String info = succeString; //发送tcp连接成功消息
        pw.println(info); //向客户端返回用户发送的消息，println输出完后会自动刷新缓冲区
        pw.flush();
        Thread.sleep(10);
    }
```