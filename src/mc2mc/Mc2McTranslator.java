package mc2mc;

import com.beust.jcommander.JCommander;
import mc2mc.analysis.TirAnalysis;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.ReadOptions;

public class Mc2McTranslator {

    public Mc2McTranslator(){
        // constructor
    }

    public static void main(String[] args) {
        if(args.length == 0){
            PrintMessage.Error("No options given\\nTry --help for usage");
        }


        ReadOptions options = new ReadOptions();
        JCommander jcommander = new JCommander(options, args);
        jcommander.setProgramName("Mc2Mc");

        PrintMessage.welcomeWords();
        if(options.isOptViewer){

        }
        else {
            if (options.outDir.equals("")) {
                PrintMessage.Error("Please provide a valid output directory by using -out \"dir\"");
            } else if (options.outDir.equals("./")) {
                PrintMessage.Error("The directory ./ is not acceptable\n" +
                        "Please provide a valid output directory by using -out \"dir\"");
            }
        }

        String[] parameters = options.arguments.split(" ");
        PrintMessage.arrayList(parameters, "Option information");

        runOptions(options, jcommander);

//        int argSize =args.length;
//        String[] parameters = options.arguments.subList(1,argSize).toArray(new String[0]);
        String mainFile = options.inputArgs.get(0);
        String outDir = options.outDir;
        int cnt = 0;
        while(true) {
            PrintMessage.See(cnt+"", "Round");
            TirAnalysis tira = new TirAnalysis(mainFile, parameters, outDir);
            if(options.isOptViewer){
                tira.RunTamerViewer();
                break;
            }
            else {
                tira.runAnalysis(); //main analysis
                if (tira.getNoChange()) break;
                mainFile = tira.getOutPath();
            }
//            if(cnt == 0) break;
            cnt++;
        }
        PrintMessage.See("Exit successfully");
        //tira.TestTirFunction();
        //tira.TirValueAnalysis();
        //tira.TestLocalAnalysis();

    }

    public static void runOptions(ReadOptions opt, JCommander jc){
        if(opt.isHelp){
            jc.usage();
            System.exit(0);
        }

//        if(opt.isOptDisplay){
//            PrintMessage.See("Optimization options:\n\n" +
//                    "1) If conversion\n" +
//                    "2) Function vectorization");
//            System.exit(0);
//        }
    }

}
