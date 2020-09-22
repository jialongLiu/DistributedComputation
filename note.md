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