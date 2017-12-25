import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.audio.mp3.MP3AudioHeader;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.TagException;
import java.io.*;
import java.util.ArrayList;


/*
*  功能：删除mp3音频文件的头尾广告
*  该类也可以用来处理ts流等切片后不影响播放的文件
*
*  使用方法：初始化Mp3Process类，传入源文件路径、目标路径、需要删除的头尾时长（单位：秒）
*  然后调用  process()方法即可
*/
public class Mp3Process {
    private static final String curPaht = System.getProperty("user.dir");//获取当前路径
    private String inputPath;
    private String outputPath;
    private int needRemoveBeginSecondLength;//单位秒
    private int needRemoveEndTimeSecondLength;//单位秒
    private final static String FILE_TYPE = ".mp3";

    public Mp3Process(String inputPath, String outputPath, int beginSecondLength, int endSecondLength) {
        this.inputPath = checkInputPath(inputPath);
        this.outputPath = checkOutputPath(outputPath);
        this.needRemoveBeginSecondLength = beginSecondLength <= 0 ? 0 : beginSecondLength;
        this.needRemoveEndTimeSecondLength = endSecondLength <= 0 ? 0 : endSecondLength;
        createDirs(this.outputPath);//创建输出文件夹
    }

    public void process() {
        //递归查找后缀为mp3的文件
        ArrayList<File> fileArrayList = findMp3Files(this.inputPath, new ArrayList<File>());
        for (File file:fileArrayList){
            generateNewMp3File(file);//删除头尾，生成新的文件
        }
        System.out.println("--------------------------------------------");
        System.out.println("处理完毕!!!!");
        System.out.println("新的文件位于: " + outputPath);
    }

    /*
    *    递归该目录及其子目录，找出所有的mp3文件进行处理
     */
    private ArrayList<File> findMp3Files(String filePath, ArrayList<File> fileArrayList){
        try {
            File file = new File(filePath);
            if (file.exists()){
                if (file.isDirectory()){
                    String[] filesName = file.list();
                    for (String name:filesName){
                        findMp3Files(file.getAbsolutePath() + "\\" + name, fileArrayList);//递归读取文件夹
                    }
                }else {//普通文件
                    //如果是mp3文件，才需要处理
                    if (isMp3File(file.getName())){
                        fileArrayList.add(file);
                    }
                }
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }catch (SecurityException e){
            e.printStackTrace();
        }
        return fileArrayList;
    }

    private static boolean isMp3File(String name){
        return name.toLowerCase().contains(FILE_TYPE);
    }

    /*
    *   该应用的核心方法
    *   读取mp3数据，过滤掉开头和结尾需要删除的数据，生成新的mp3文件
    */
    private void generateNewMp3File(File file){
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        final long bytesPerSecond = getBytesPerSecond(file);
        final long delStartByteNum = this.needRemoveBeginSecondLength * bytesPerSecond;
        final long delEndByteNum = this.needRemoveEndTimeSecondLength * bytesPerSecond;

        try {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            inputStream.skip(delStartByteNum);//跳过起始长度
            outputStream = new BufferedOutputStream(new FileOutputStream(getOutputFile(file)));

            byte[] buffer = new byte[1024];//每次读取1024字节
            long readCount = (file.length() - delStartByteNum - delEndByteNum)/buffer.length;
            for (int i=0; i< readCount; i++){
                if (-1 != inputStream.read(buffer)){
                    outputStream.write(buffer);//写数据
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            try {
                if (inputStream != null){
                    inputStream.close();
                }
                if (outputStream != null){
                    outputStream.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    /*
    *   建立新的文件，例如：原文件名为   abcd.mp3   建立的新文件为 abcd_new.mp3
     */
    private File getOutputFile(File inputFile){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.outputPath).append("\\");
        stringBuilder.append(inputFile.getName().substring(0, inputFile.getName().length()-FILE_TYPE.length()));
        stringBuilder.append("_new");
        stringBuilder.append(inputFile.getName().substring(inputFile.getName().length()-FILE_TYPE.length()));
        File outputFile = new File(stringBuilder.toString());
        try {
            outputFile.createNewFile();//创建新的文件,用于存储需要需要读取的数据
        }catch (IOException e){
            e.printStackTrace();
        }
        return  outputFile;
    }

    /*
    *  获取mp3的时长
    */
    private static int getMp3TrackLength(File mp3File){
        int trackLength = 0;
        try {
            MP3File file = (MP3File)AudioFileIO.read(mp3File);
            MP3AudioHeader audioHeader = file.getMP3AudioHeader();
            trackLength = audioHeader.getTrackLength();//单位  秒
        }catch (CannotReadException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }catch (ReadOnlyFileException e){
            e.printStackTrace();
        }catch (InvalidAudioFrameException e){
            e.printStackTrace();
        }catch (TagException e){
            e.printStackTrace();
        }
        return trackLength;
    }

    /*
    *   每秒字节数，这样可以根据要删除时长算出要删除的字节数
     */
    private static long getBytesPerSecond(File file){
        long fileLength = file.length();
        long trackLength = getMp3TrackLength(file);

        return fileLength/trackLength;
    }

    /*
    *   调试用打印
    */
    private static void printFileDuration(int duration){
        int hour = duration/60/60;
        int min = (duration - hour*60)/60;
        int second = duration%60;
        System.out.println("文件时长   "+hour+":"+min+":"+second+"\r\n");
    }

    /*
    *   如果没有输入路径，则默认当前路径
    *   如果输入的相对路径，拼接成绝对路径
    *   如果已经是绝对路径，设置为该绝对路径
    */
    private static String checkInputPath(String inputPath) {
        String path = "";
        if ((null == inputPath) || "".equals(inputPath)) {//没有指定输入路径，默认当前路径
            path = curPaht;
        } else if (!inputPath.contains(":")) {//不包含冒号，是相对路径
            path = curPaht + "\\" + inputPath;
        }else {
            path = inputPath;
        }
        return path;
    }

    /*
    *   如果没有输入路径，则默认当前路径下，文件夹名为new的子目录
    *   如果输入的相对路径，拼接成绝对路径
    *   如果已经是绝对路径，设置为该绝对路径
    */
    private static String checkOutputPath(String outputPath) {
        String path = "";
        if ("".equals(outputPath)) {//没有指定输出路径，默认当前路径的子文件夹new
            path = curPaht + "\\new";//当前路径的子目录new文件夹
        } else if (!outputPath.contains(":")) {//不包含冒号，是相对路径
            path = curPaht + "\\" + outputPath;
        }else {
            path = outputPath;
        }
        return path;
    }

    private static boolean createDirs(String path){
        File file = new File(path);
        return file.mkdirs();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(1024);
        stringBuilder.append("输入路径：").append(this.inputPath).append("\r\n");
        stringBuilder.append("输出路径：").append(this.outputPath).append("\r\n");
        stringBuilder.append("要删除的开头时长：").append(this.needRemoveBeginSecondLength).append("s\r\n");
        stringBuilder.append("要删除的结尾时长：").append(this.needRemoveEndTimeSecondLength).append("s\r\n");
        return stringBuilder.toString();
    }
}
