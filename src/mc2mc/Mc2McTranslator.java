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

        runOptions(options, jcommander);

        if(options.arguments.size() == 2) {
            //String parametern = "DOUBLE&1*1&REAL";
            String[] parameters = options.arguments.subList(1,2).toArray(new String[0]);
            PrintMessage.Strings(parameters);
            TirAnalysis tira = new TirAnalysis(options.arguments.get(0), parameters);
            //tira.TestTirFunction();
            //tira.TirValueAnalysis();
            //tira.TestLocalAnalysis();
            tira.RunLoopInvariant();
        }
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
