import java.io.FileReader;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.systemj.CompilationUnit;
import org.systemj.CompilerPrintStream;

import args.Helper;

import com.google.gson.Gson;

/**
 * HMPSoC main class
 * @author hpar081
 *
 */
public class HMPSoC {



	public static Options initOptions() {
		Options options = new Options();
		options.addOption(Option.builder(Helper.VERBOSE_OPTION).longOpt(Helper.VERBOSE_LONG_OPTION).desc("Verbose mode").build());
		options.addOption(Option.builder(Helper.D_OPTION).hasArg().argName("directory").desc("Generate files to this output directory").build());
		options.addOption(Option.builder(Helper.JOP_RECOP_NUM_OPTION).hasArg().argName("alloc").desc("Specify JOP/ReCOP configuration").build());
		options.addOption(Option.builder(Helper.HELP_OPTION).longOpt(Helper.HELP_LONG_OPTION).desc("Print this help message").build());
		return options;
	}

	public static void printHelp(Options options) {
		HelpFormatter hf = new HelpFormatter();
		hf.setOptionComparator(new Comparator<Option>() {
			@Override
			public int compare(Option o1, Option o2) {
				if(o1.getOpt().equals(Helper.HELP_OPTION)){
					return 1;
				}
				else if(o2.getOpt().equals(Helper.HELP_OPTION)){
					return -1;
				}
				return 0;
			}
		});
		CompilerPrintStream.setVerbose();
		hf.printHelp("java -jar hmpsoc.jar [OPTIONS] <filename>", options);
		CompilerPrintStream.resetVerbose();
	}

	public static void main(String[] args) {
		CompilerPrintStream cps = new CompilerPrintStream(System.out);
		CompilerPrintStream cpser = new CompilerPrintStream(System.err);
		System.setOut(cps);
		System.setErr(cpser);
		Options options = initOptions();

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
			Helper.setSingleArgInstance(cmd);

			for(Option o : cmd.getOptions()){
				if(o.getOpt().equals(Helper.HELP_OPTION)){
					printHelp(options);
					System.exit(0);
				}
				else{
					switch(o.getOpt()){
					case Helper.VERBOSE_OPTION:
						CompilerPrintStream.setVerbose();
						CompilerPrintStream.setDefaultVerbose();
						break;
					case Helper.JOP_RECOP_NUM_OPTION:
						String fname = o.getValue(Helper.JOP_RECOP_NUM_OPTION);
						Gson gs = new Gson();
						Helper.pMap = gs.fromJson(new FileReader(fname), Helper.Mapping.class);
						if(Helper.pMap.rAlloc == null)
							throw new RuntimeException("Required data in "+fname+" : rAlloc");
						Iterator<Integer> i = (Iterator<Integer>) Helper.pMap.rAlloc.values().iterator();
						while(i.hasNext()){
							int nn = i.next();
							if(Helper.pMap.nReCOP < nn)
								throw new RuntimeException("ReCOP ID "+nn+" is greater than the max # of ReCOP: "+Helper.pMap.nReCOP);
						}
						if(Helper.pMap.nJOP < 0 || Helper.pMap.nReCOP < 0){
							throw new ParseException("Numbers of JOP/ReCOP should be greater than 0");
						}
					default:
						break;
					}
				}
			}

			List<String> arglists = cmd.getArgList();
			if(!arglists.isEmpty()){
				for(String f : arglists){
					CompilationUnit cu = new CompilationUnit(f);
					cu.process();
				}
			}
			else{
				CompilationUnit cu = new CompilationUnit(System.in);
				cu.process();
			}
		} catch (Exception e){
			CompilerPrintStream.setVerbose();
			if(cmd.hasOption(Helper.VERBOSE_OPTION))
				e.printStackTrace();
			else
				System.err.println(e.getMessage());

			System.exit(1);
		} catch (Error e){
			CompilerPrintStream.setVerbose();
			e.printStackTrace();
			System.exit(1);
		}
	}
}
