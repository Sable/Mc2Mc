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

        PrintMessage.arrayList(options.arguments, "Option information");

        runOptions(options, jcommander);

        int argSize =options.arguments.size();
        String[] parameters = options.arguments.subList(1,argSize).toArray(new String[0]);
        String mainFile = options.arguments.get(0);
        TirAnalysis tira = new TirAnalysis(mainFile, parameters);
        //tira.TestTirFunction();
        //tira.TirValueAnalysis();
        //tira.TestLocalAnalysis();

        if(options.isOptViewer){
            tira.RunTamerViewer();
        }
        else {
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
