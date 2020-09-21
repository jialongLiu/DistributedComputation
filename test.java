import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class test {
    public static void main(String[] args) throws IOException {
        
        // // 测试输出
        // System.out.println("sjdfihsdohfiosdif");
        
        // // 测试输出string[]
        // String[] strarray = {"liu","jia","long"};
        // for(int i = 0; i<strarray.length;i++){
        //     System.out.println(strarray[i]);
        // }
        
        // // 测试string[]转换为list并输出
        // List list = Arrays.asList(strarray);
        // System.out.println(list);
        
        // // 测试list转换为string[]并输出
        // String[] strarray2 = new String[list.size()];
        // list.toArray(strarray2);
        // for(int i = 0; i<strarray2.length;i++){
        //     System.out.println(strarray2[i]);
        // }

        // // 测试当前目录
        // File f= new File("");
        // System.out.println(f.getAbsolutePath());

        // // 测试nextline和next输出
        // Scanner sc = new Scanner(System.in);
        // // BufferedReader br = new BufferedReader(sc);
        // System.out.println(sc.next());
        // System.out.println(sc.next());
        // sc.close();

        // //测试file当前目录
        // File f= new File("");
        // System.out.println(f.getAbsolutePath());
        // File getfile = new File(f.getAbsolutePath()+"\\"+"dfs");
        // // FileInputStream fileInput = new FileInputStream(getfile);
        // System.out.println(getfile.getAbsolutePath());
        // System.out.println(getfile.isFile());
        // // System.out.println(fileInput);

        // //测试文件转换为byte[]
        // File f =new File("test.java");
        // System.out.println(f.getAbsolutePath()); 
        // FileInputStream finput = new FileInputStream(f);
        // byte[] fileByte = new byte[1024];
        // System.out.println(finput.read(fileByte));
        // System.out.println(fileByte[5]);

        // //测试sccaner
        // while(true){
        //     Scanner sc = new Scanner(System.in,"GBK");
        //     System.out.println("you said:"+sc.nextLine()); //next中文乱码，会把它识别为空格。所以读不出来。nextline虽然也会乱码。但是不会停止。
        // }
        
    }
}
