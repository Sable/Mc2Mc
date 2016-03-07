package mc2mc;

import com.beust.jcommander.JCommander;
import mc2mc.mc2lib.PrintMessage;
import mc2mc.mc2lib.ReadOptions;

public class Mc2McTranslator {

    public Mc2McTranslator(){
        // constructor
    }

    public static void main(String[] args) {
        setOptions(args);
        /*if(args.length == 1) {

            String parametern = "DOUBLE&1*1&REAL";
            TirAnalyses tira = new TirAnalyses(args[0],parametern);
            //tira.TestTirFunction();
            //tira.TirValueAnalysis();
            tira.TestLocalAnalysis();
        }*/
    }

    public static void setOptions(String[] args){
        if(args.length == 0){
            PrintMessage.Error("No options given\\nTry --help for usage");
        }

        ReadOptions options = new ReadOptions();
        JCommander jcommander = new JCommander(options, args);
        jcommander.setProgramName("Mc2Mc");

        runOptions(options, jcommander);

    }

    public static void runOptions(ReadOptions opt, JCommander jc){
        if(opt.isHelp){
            jc.usage();
            System.exit(0);
        }

        if(opt.isOptDisplay){
            PrintMessage.See("Optimization options:\n\n" +
                    "1) If conversion\n" +
                    "2) Function vectorization");
            System.exit(0);
        }
    }

}
