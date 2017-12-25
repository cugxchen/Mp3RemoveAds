import java.util.Scanner;

public class Client {
    private static String inputPath;
    private static String outputPaht;
    private static int needRemoveBeginSecondLength;//单位秒
    private static int needRemoveEndTimeSecondLength;//单位秒

    public static void main(String[] args) {
        initParamFromConsole();

        Mp3Process mp3Process = new Mp3Process(inputPath, outputPaht, needRemoveBeginSecondLength, needRemoveEndTimeSecondLength);
        System.out.println(mp3Process);
        mp3Process.process();

        stayOnConsole();
    }

    private static void initParamFromConsole(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入读取路径(直接回车默认在当前路径)：");
        inputPath = scanner.nextLine();
        System.out.println("请输入生成路径(直接回车默认在当前路径的new文件夹)：");
        outputPaht = scanner.nextLine();
        System.out.println("请输入需要删除的起始时长(单位 秒)：");
        needRemoveBeginSecondLength = scanner.nextInt();
        System.out.println("请输入需要删除的结束时长(单位 秒)：");
        needRemoveEndTimeSecondLength = scanner.nextInt();
    }

    private static void stayOnConsole(){
        Scanner scanner = new Scanner(System.in);
        scanner.next();
    }
}
