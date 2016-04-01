import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Map;

public class PipairD {
	
	/* store function(s) called by it(their) caller(s) as value in HashMap pipair, 
	 * use HashSet to eliminate duplicate functions */
	public static Set<String> stringset = new HashSet<String>();
	
	/* record function(s) and it(their) caller(s) as key-value set in Map*/
	public static Map<ArrayList<String>, HashSet<String>> pipair = new HashMap<ArrayList<String>, HashSet<String>>();
	
	public static void main(String [] args) throws Exception{
		
		int support = 3;
		float confidence = (float)0.65;
		int expansion=0;
		int latent_spe=1;
		
		switch(args.length){
			//case 0: break;
			case 1: break;
			case 3:
				support = Integer.parseInt(args[1]);
				confidence = Float.parseFloat(args[2])/100;
				break;
			case 4:
				support = Integer.parseInt(args[1]);
				confidence = Float.parseFloat(args[2])/100;
				expansion=Integer.parseInt(args[3]);
				break;
			case 5: 
				support = Integer.parseInt(args[1]);
				confidence = Float.parseFloat(args[2])/100;
				expansion=Integer.parseInt(args[3]);
				latent_spe = Integer.parseInt(args[4]);
				break;
			default:
				System.err.println("Error: Wrong arguments input.");
                System.exit(1);
		}
		
		/* read callgraph file into program and store in map */
		readFile(expansion);
		if(latent_spe==0)
			findBug(support, confidence);
		else
			findBug_latent_spe(support,confidence);  //find bug using Latent Specification
		
		return;
	}

	static void readFile(int expansion) throws IOException{
		
		HashMap<String,ArrayList<String>>  caller_to_callee=new HashMap<String,ArrayList<String>>(); //save all caller_callee relationship
		String caller;
		
		//File file = new File("src/q1/graphoutput.txt");
		//Scanner scanner = new Scanner(file);
		Scanner scanner = new Scanner(System.in);
		
		String pattern_caller = "Call.*'(.*)'.*";
		Pattern p_caller = Pattern.compile(pattern_caller);
		Matcher m_caller = null;
		
		String pattern_callee = ".*calls.*'(.*)'.*";
		Pattern p_callee = Pattern.compile(pattern_callee);
		Matcher m_callee = null;
		
		String line = null; 
		String add_value = null;
		String add_key = null;

		while(scanner.hasNext()){
			ArrayList<String> callee_temp=new ArrayList<String>();  //must do duplicate set instantiation before map.put every time
			line = scanner.nextLine();
			m_caller = p_caller.matcher(line);
			if(m_caller.matches()){
				add_value = m_caller.group(1);
				stringset.clear();
				while(scanner.hasNext()){
					line = scanner.nextLine();
					if(line.length() == 0){
						break;
					}
					
					m_callee = p_callee.matcher(line);
					if(m_callee.matches()){
						add_key = m_callee.group(1);
						callee_temp.add(add_key);     //save callee to temp
						if(!stringset.contains(add_key)){
							addToMap(add_key, add_value);
							stringset.add(add_key);
						}
					}
				}

				caller_to_callee.put(add_value, callee_temp);  //save caller-[callee] pairs
			}
		}
		scanner.close();

		//loop through all caller-callee pairs to the expansion level
		ArrayList<String> callee_temp1=new ArrayList<String>();
		for(int i=0;i<expansion;i++){
			HashMap<String,ArrayList<String>>  caller_to_callee_temp=new HashMap<String,ArrayList<String>>();
			for(Map.Entry<String, ArrayList<String>> entry:caller_to_callee.entrySet()){
				caller=entry.getKey();
				callee_temp1=entry.getValue();
				ArrayList<String> callee_temp2=new ArrayList<String>();
				for(String callee:callee_temp1){
					callee_temp2.addAll(caller_to_callee.get(callee));
				}
				caller_to_callee_temp.put(caller, callee_temp2);
				stringset.clear();
				for(String callee2:callee_temp2){
					stringset.add(callee2);
					addToMap(callee2,caller);
				}
					
			}
			caller_to_callee=caller_to_callee_temp;
		}
		return;
	}
	
	static void addToMap(String add_key, String add_value){
		
		HashSet<String> map_value = null;
		ArrayList<String> map_key = new ArrayList<String>();
		
 		/* map_key only has one element */
 		map_key.add(add_key);
 		if(pipair.containsKey(map_key)){
			map_value = pipair.get(map_key);
		} else{
			map_value = new HashSet<String>();
		}
		map_value.add(add_value);
		
		pipair.put(map_key, map_value);
		
		/* map_key has two elements */
		for(String key:stringset){
			if (key==add_key) continue;
			map_key = new ArrayList<String>();
			map_key.add(key);
			map_key.add(add_key);
			if(pipair.containsKey(map_key)){
				map_value = pipair.get(map_key);
			} else{
				map_value = new HashSet<String>();
			}
			map_value.add(add_value);
			
			pipair.put(map_key, map_value);
		}
		
		return;
	}
	
	//find bug without using Latent Specification	
	static void findBug(int support, float confidence){
		int count=0;
		/*  two HashMap separate pipairs whose keys include one element and two, 
		 *  key is the same as that in pipair,
		 *  value is the number of elements of the corresponding value in pipair */
		HashMap<ArrayList<String>, Integer> pipair_single = new HashMap<ArrayList<String>, Integer>();
		HashMap<ArrayList<String>, Integer> pipair_pair = new HashMap<ArrayList<String>, Integer>();
		
		/* construct pipair_single and pipair_pair from pipair */
		for(Map.Entry<ArrayList<String>, HashSet<String>> entry : pipair.entrySet()){
			ArrayList<String> key = entry.getKey();
			HashSet<String> value = entry.getValue();
			int key_count = key.size();
			int value_count = value.size();
			if(key_count == 1){
				pipair_single.put(key, value_count);
			} else if(key_count == 2){
				pipair_pair.put(key, value_count);
			} else{
				System.err.println("Error");  ////// throw Exception
			}
		}
		
		for(Map.Entry<ArrayList<String>, Integer> entry_single : pipair_single.entrySet()){
			int value_single = entry_single.getValue();
			ArrayList<String> key_s = entry_single.getKey();
			String key_single = key_s.toString();
			key_single = key_single.replaceAll("\\[", "");
			key_single = key_single.replaceAll("\\]", "");
			
			if(value_single >= support){
				for(Map.Entry<ArrayList<String>, Integer> entry_pair : pipair_pair.entrySet()){
					int value_pair = entry_pair.getValue();
					ArrayList<String> key_pair = entry_pair.getKey();
					double confi = (float)value_pair / value_single;
					
					if(value_pair >= support && key_pair.contains(key_single) && confi >=confidence && confi < 1){		
						HashSet<String> single = null;
						HashSet<String> pair = null;
						single = pipair.get(key_s);  // not key_single, it is a String not HashSet
						pair = pipair.get(key_pair);
						for(String s:single){
							if(!pair.contains(s)){
								/* printing bug */
								NumberFormat fmt = NumberFormat.getPercentInstance();
								fmt.setMinimumFractionDigits(2);
								fmt.setMaximumFractionDigits(2);
								
								String bug_pair=key_pair.toString();
								bug_pair = bug_pair.replaceAll("\\[", "(");
								bug_pair = bug_pair.replaceAll("\\]", ")");
								
								System.out.println("bug: " + key_single + " in "+ s 
									+ ", pair: " + bug_pair
									+ ", support: " + value_pair
									+ ", confidence: " + fmt.format(confi)
									);
								count++;
							}	
						}
					}
				}
			}
	
		} // end of for loop 
		System.out.println(count);
	} // end of function findBug
	
	//find bug using Latent Specification
	static void findBug_latent_spe(int support, float confidence){
		//int count=0;
		
		String[] keywords={"lock","unlock","alloc","free","release"};   //The key words used in Latent Specification
		/*  two HashMap separate pipairs whose keys include one element and two, 
		 *  key is the same as that in pipair,
		 *  value is the number of elements of the corresponding value in pipair */
		HashMap<ArrayList<String>, Integer> pipair_single = new HashMap<ArrayList<String>, Integer>();
		HashMap<ArrayList<String>, Integer> pipair_pair = new HashMap<ArrayList<String>, Integer>();
		
		/* construct pipair_single and pipair_pair from pipair */
		for(Map.Entry<ArrayList<String>, HashSet<String>> entry : pipair.entrySet()){
			ArrayList<String> key = entry.getKey();
			HashSet<String> value = entry.getValue();
			int key_count = key.size();
			int value_count = value.size();
			if(key_count == 1){
				pipair_single.put(key, value_count);
			} else if(key_count == 2){
				pipair_pair.put(key, value_count);
			} else{
				System.err.println("Error");  ////// throw Exception
			}
		}
		
		/*  key_pair<HashSet<String>> -> value_pair<Integer>,
		 *  key_single<String> -> value_single<Integer>  
		 * 
		 *  key_pair<HashSet<String>> -> pair<HashSet<String>>, 
		 *  key_single<String> -> single<HashSet<String>> */
		for(Map.Entry<ArrayList<String>, Integer> entry_single : pipair_single.entrySet()){
			int value_single = entry_single.getValue();
			ArrayList<String> key_s = entry_single.getKey();
			String key_single = key_s.toString();
			key_single = key_single.replaceAll("\\[", "");
			key_single = key_single.replaceAll("\\]", "");
			
			if(value_single >= support){
				for(Map.Entry<ArrayList<String>, Integer> entry_pair : pipair_pair.entrySet()){
					int value_pair = entry_pair.getValue();
					ArrayList<String> key_pair = entry_pair.getKey();
					double confi = (float)value_pair / value_single;
					for (String words:key_pair){
						for (String keyword:keywords){
							int index=words.indexOf(keyword);
							if (index!=-1) {
								confi=Math.sqrt(confi);
								break;
							}
						}
					}
					
					if(value_pair >= support && key_pair.contains(key_single) && confi >=confidence && confi <= 1){		
						HashSet<String> single = null;
						HashSet<String> pair = null;
						single = pipair.get(key_s);  // not key_single, it is a String not HashSet
						pair = pipair.get(key_pair);
						for(String s:single){
							if(!pair.contains(s)){
								/* printing bug */
								NumberFormat fmt = NumberFormat.getPercentInstance();
								fmt.setMinimumFractionDigits(2);
								fmt.setMaximumFractionDigits(2);
								
								String bug_pair=key_pair.toString();
								bug_pair = bug_pair.replaceAll("\\[", "(");
								bug_pair = bug_pair.replaceAll("\\]", ")");
								
								System.out.println("bug: " + key_single + " in "+ s 
									+ ", pair: " + bug_pair
									+ ", support: " + value_pair
									+ ", confidence: " + fmt.format(confi)
									);
								//count++;
							}	
						}
					}
				}
			}
	
		} // end of for loop 
		//System.out.println(count);
	} // end of function findBug_latent_spe	
} // end of class PiPair