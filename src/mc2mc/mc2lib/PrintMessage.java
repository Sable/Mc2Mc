package mc2mc.mc2lib;

public class PrintMessage {
    public static void Error(String text){
        System.err.println(text);
        System.exit(99);
    }

    public static void Warning(String text){
        System.err.println(text);
    }

    public static void See(String text){
        System.out.println(text);
    }
}