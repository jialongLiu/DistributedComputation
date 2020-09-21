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