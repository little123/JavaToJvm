package codeIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import struct.Constant;
import struct.InsCode;

public class JavaToHs {
	private static List<Constant> list;
	private static String path = "C:\\Users\\liu\\Desktop\\isabelle\\Jinja\\JVM\\haskell\\";
	private static String filenameTemp;

	private static Map<String, List<String>> classAndMethodsMap = new HashMap<String, List<String>>();
	private static Map<String, List<String>> classAndfieldsMap = new HashMap<String, List<String>>();
	private static Map<String, InsCode> InscodeMap = new HashMap<String, InsCode>();

	private static String insKey;
	private static InsCode insCode;
	private static String classString = "";
	private static String methodString = "";
	private static boolean isCodeBegin = false;
	private static String code = "";

	/**
	 * 功能：Java读取txt文件的内容 步骤：1：先获得文件句柄 2：获得文件句柄当做是输入一个字节码流，需要对这个输入流进行读取
	 * 3：读取到输入流后，需要读取生成字节流 4：一行一行的输出。readline()。 备注：需要考虑的是异常情况
	 * 
	 * @param filePath
	 */
	public static void readTxtFile(String filePath) {
		try {
			String encoding = "GBK";
			File file = new File(filePath);
			if (file.isFile() && file.exists()) { // 判断文件是否存在
				InputStreamReader read = new InputStreamReader(new FileInputStream(file), encoding);// 考虑到编码格式
				BufferedReader bufferedReader = new BufferedReader(read);
				String lineTxt = null;
				while ((lineTxt = bufferedReader.readLine()) != null || isCodeBegin) {
					if (lineTxt.equals("Constant pool:")) {
						list = new ArrayList<>();
					}
					if (lineTxt.contains("#") && lineTxt.contains("=")) {

						Constant constant = new Constant();
						int numStartIndex = lineTxt.indexOf("#");
						int numEndIndex = lineTxt.indexOf("= ");
						String numString = lineTxt.substring(numStartIndex + 1, numEndIndex - 1);
						constant.setNum(numString);

						int keyStartIndex = numEndIndex;
						int keyEndIndex = keyStartIndex + 18;
						String key = lineTxt.substring(keyStartIndex + 1, keyEndIndex);
						key = key.replace(" ", "");
						constant.setKey(key);

						int valueStartIndex;
						if (lineTxt.contains("//")) {
							valueStartIndex = lineTxt.indexOf("//") + 2;
						} else {
							valueStartIndex = keyEndIndex + 2;
						}
						int valueEndIndex = lineTxt.length();
						String value = lineTxt.substring(valueStartIndex + 1, valueEndIndex);
						constant.setValue(value);

						list.add(constant);
					}

					// 将class,method,field存入map
					if (lineTxt.contains("{")) {
						for (Constant constant : list) {
							if (constant.getKey().equals("Methodref") && !constant.getValue().contains("java/")) {
								saveMethodsOrFields(constant, classAndMethodsMap);
							} else if (constant.getKey().equals("Fieldref") && !constant.getValue().contains("java/")) {
								saveMethodsOrFields(constant, classAndfieldsMap);
							}
						}
					}

					// 将ins存入map
					if (lineTxt.contains(");")) {
						if (lineTxt.contains("main")) {
							insKey = "main";
						} else {
							boolean flag = false;
							for (Entry<String, List<String>> camEntry : classAndMethodsMap.entrySet()) {
								if (lineTxt.contains(camEntry.getKey())) {
									if (!classString.contains(camEntry.getKey() + ",")) {
										insKey = camEntry.getKey();
										classString = classString + insKey + ",";
										flag = true;
									}
								} else {
									List<String> methodList = camEntry.getValue();
									for (String method : methodList) {
										if (!methodString.toString().contains(method + ",")) {
											insKey = method;
											methodString = methodString + insKey + ",";
											flag = true;
											break;
										}
									}
								}

								if (flag) {
									break;
								}
							}
						}

						insCode = new InsCode();

						// 获取方法参数
						List<String> insParams = new ArrayList<String>();
						String params = "";
						int paramsSatarIndex = lineTxt.indexOf('(');
						int paramsEndIndex = lineTxt.indexOf(')', paramsSatarIndex);
						params = lineTxt.substring(paramsSatarIndex + 1, paramsEndIndex);

						String[] ps = params.split(",");
						for (String paramStr : ps) {
							if (paramStr.trim().equals("int")) {
								insParams.add("Integer");
							} else {
								if (paramStr.contains(".")) {
									int index = paramStr.lastIndexOf('.');
									String typeClass = paramStr.substring(index + 1, paramStr.length());
									insParams.add("Class " + typeClass + "_name");
								} else {
									insParams.add("");
								}
							}
						}

						if (insParams.isEmpty() || insParams == null) {
							insParams.add("");
						}

						insCode.setParams(insParams);
					}

					if (lineTxt.contains("descriptor")) {
						String type = "";
						int rTypeStartIndex = lineTxt.indexOf(')');
						int rTypeEndIndex = lineTxt.length();
						String rType = lineTxt.substring(rTypeStartIndex + 1, rTypeEndIndex);

						if (rType.equals("I")) {
							type = "Integer";
						} else if (rType.equals("V")) {
							type = "Void";
						} else if (rType.contains(";")) {
							int classStartIndex = lineTxt.lastIndexOf('/');
							int classEndIndex = lineTxt.lastIndexOf(';');
							type = "Class " + lineTxt.substring(classStartIndex + 1, classEndIndex);
						}

						insCode.setReturnType(type);
					}
					if (lineTxt.contains("stack=")) {
						int[] args = getInitStat(lineTxt);
						insCode.setStack(args[0]);
						insCode.setLocals(args[1]);
						insCode.setArgs_size(args[2]);
						isCodeBegin = true;
					}

					if (isCodeBegin && lineTxt.contains(":")) {
						code = code + translateCode(lineTxt) + "\n";
					}

					if ((lineTxt.equals("") || lineTxt.isEmpty() || lineTxt.contains("}")) && isCodeBegin) {
						isCodeBegin = false;
						insCode.setCode(code.toString());
						InscodeMap.put(insKey, insCode);
						code = "";
					}

					System.out.println(lineTxt);
				}
				read.close();
			} else {
				System.out.println("找不到指定的文件");
			}
		} catch (

		Exception e) {
			System.out.println("读取文件内容出错");
			e.printStackTrace();
		}

	}

	public static String translateCode(String jCode) {
		String jvmCode = "";
		String value = "";
		if (jCode.contains("_")) {
			int start = jCode.indexOf('_');
			value = jCode.substring(start + 1, jCode.length());
		}
		if (jCode.contains("load")) {
			jvmCode = "        Load" + " " + value + ",";
		} else if (jCode.contains("store")) {
			jvmCode = "        Store" + " " + value + ",";
		} else if (jCode.contains("const")) {
			jvmCode = "        Push" + " " + value + ",";
		} else if (jCode.contains("get")) {
			String num = "";
			int numStartIndex = jCode.indexOf('#') + 1;
			int numEndIndex = jCode.indexOf(" ", numStartIndex);
			num = jCode.substring(numStartIndex, numEndIndex);

			String cname = "";
			String fname = "";
			for (Constant constant : list) {
				if (constant.equals(num)) {
					String consValue = constant.getValue();
					int cStartIndex = consValue.indexOf('/');
					int cEndIndex = consValue.indexOf(cStartIndex, '.');
					cname = consValue.substring(cStartIndex + 1, cEndIndex);

					int mStartIndex = cEndIndex + 1;
					int mEndIndex = consValue.indexOf(':');
					fname = consValue.substring(mStartIndex, mEndIndex);
				}
			}

			jvmCode = "        Getfield" + " " + fname + " " + cname + ",";
		} else if (jCode.contains("put")) {
			String num = "";
			int numStartIndex = jCode.indexOf('#') + 1;
			int numEndIndex = jCode.indexOf(" ", numStartIndex);
			num = jCode.substring(numStartIndex, numEndIndex);

			String cname = "";
			String fname = "";
			for (Constant constant : list) {
				if (constant.equals(num)) {
					String consValue = constant.getValue();
					int cStartIndex = consValue.indexOf('/');
					int cEndIndex = consValue.indexOf(cStartIndex, '.');
					cname = consValue.substring(cStartIndex + 1, cEndIndex);

					int fStartIndex = cEndIndex + 1;
					int fEndIndex = consValue.indexOf(':');
					fname = consValue.substring(fStartIndex, fEndIndex);
				}
			}

			jvmCode = "        Putfield" + " " + fname + " " + cname + ",";
		} else if (jCode.contains("invoke")) {
			String num = "";
			int numStartIndex = jCode.indexOf('#') + 1;
			int numEndIndex = jCode.indexOf(" ", numStartIndex);
			num = jCode.substring(numStartIndex, numEndIndex);

			String mname = "";
			Constant constant = list.get(Integer.valueOf(num) - 1);
			String consValue = constant.getValue();
			int mStartIndex = consValue.lastIndexOf('.');
			int mEndIndex = consValue.indexOf(":");
			mname = consValue.substring(mStartIndex + 1, mEndIndex);

			if (mname.contains("init")) {
				jvmCode = jCode;
			} else {
				jvmCode = "        Invoke" + " " + mname + " 1" + ",";
			}

		} else if (jCode.contains("return")) {
			jvmCode = "        Return";
		} else if (jCode.contains("pop")) {
			jvmCode = "        Pop,";
		} else if (jCode.contains("add")) {
			jvmCode = "        IAdd,";
		} else if (jCode.contains("goto")) {
			String indexNum = "";
			int valueStartIndex = jCode.indexOf('o');
			int valueEndIndex = jCode.length();
			indexNum = jCode.substring(valueStartIndex + 1, valueEndIndex).replaceAll(" ", "");
			jvmCode = "        Goto" + " " + indexNum + ",";
		} else if (jCode.contains("cmpne")) {
			String indexNum = "";
			int valueStartIndex = jCode.indexOf('e');
			int valueEndIndex = jCode.length();
			indexNum = jCode.substring(valueStartIndex + 1, valueEndIndex).replaceAll(" ", "");

			String currentIndex = "";
			int curEndIndex = jCode.indexOf(':');
			currentIndex = jCode.substring(0, curEndIndex).replaceAll(" ", "");

			int index = Integer.valueOf(indexNum) - Integer.valueOf(currentIndex);
			jvmCode = "        CmpEq," + "\n" + "IfFalse" + index + ",";
		} else if (jCode.contains("throw")) {
			jvmCode = "        Throw";
		} else if (jCode.contains("new")) {
			String num = "";
			int numStartIndex = jCode.indexOf('#') + 1;
			int numEndIndex = jCode.indexOf(" ", numStartIndex);
			num = jCode.substring(numStartIndex, numEndIndex);

			String cname = "";
			Constant constant = list.get(Integer.valueOf(num));
			String consValue = constant.getValue();
			int cStartIndex = consValue.lastIndexOf('/');
			int cEndIndex = consValue.length();
			cname = consValue.substring(cStartIndex + 1, cEndIndex);

			jvmCode = "        New" + cname + ",";

		} else {
			jvmCode = jCode;
		}

		return jvmCode;
	}

	public static int[] getInitStat(String line) {
		int[] initArgs = new int[3];
		int stackStartIndex = line.indexOf("=");
		int stackEndIndex = line.indexOf(",");
		int stack = Integer.valueOf(line.substring(stackStartIndex + 1, stackEndIndex));
		initArgs[0] = stack;

		int localsStartIndex = line.indexOf("=", stackEndIndex);
		int localsEndIndex = line.indexOf(",", localsStartIndex);
		int locals = Integer.valueOf(line.substring(localsStartIndex + 1, localsEndIndex));
		initArgs[1] = locals;

		int argsSizeStartIndex = line.indexOf("=", localsEndIndex);
		int argsSizeEndIndex = line.length();
		int argsSize = Integer.valueOf(line.substring(argsSizeStartIndex + 1, argsSizeEndIndex));
		initArgs[2] = argsSize;

		return initArgs;

	}

	/**
	 * 创建文件
	 * 
	 * @throws IOException
	 */
	public static boolean creatTxtFile(String name) throws IOException {
		boolean flag = false;
		filenameTemp = path + name + ".txt";
		File filename = new File(filenameTemp);
		if (!filename.exists()) {
			filename.createNewFile();
			flag = true;
		}
		return flag;
	}

	/**
	 * 写文件
	 * 
	 * @param newStr
	 *            新内容
	 * @throws IOException
	 */
	public static boolean writeTxtFile(String newStr) throws IOException {
		// 先读取原有文件内容，然后进行写入操作
		boolean flag = false;
		String filein = newStr + "\r\n";
		String temp = "";

		FileInputStream fis = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			// 文件路径
			File file = new File(filenameTemp);
			// 将文件读入输入流
			fis = new FileInputStream(file);
			isr = new InputStreamReader(fis);
			br = new BufferedReader(isr);
			StringBuffer buf = new StringBuffer();

			// 保存该文件原有的内容
			for (int j = 1; (temp = br.readLine()) != null; j++) {
				buf = buf.append(temp);
				// System.getProperty("line.separator")
				// 行与行之间的分隔符 相当于“\n”
				buf = buf.append(System.getProperty("line.separator"));
			}
			buf.append(filein);

			fos = new FileOutputStream(file);
			pw = new PrintWriter(fos);
			pw.write(buf.toString().toCharArray());
			pw.flush();
			flag = true;
		} catch (IOException e1) {
			// TODO 自动生成 catch 块
			throw e1;
		} finally {
			if (pw != null) {
				pw.close();
			}
			if (fos != null) {
				fos.close();
			}
			if (br != null) {
				br.close();
			}
			if (isr != null) {
				isr.close();
			}
			if (fis != null) {
				fis.close();
			}
		}
		return flag;
	}

	public static String readDate() {
		// 定义一个待返回的空字符串
		String strs = "";
		try {
			FileReader read = new FileReader(new File(filenameTemp));
			StringBuffer sb = new StringBuffer();
			char ch[] = new char[1024];
			int d = read.read(ch);
			while (d != -1) {
				String str = new String(ch, 0, d);
				sb.append(str);
				d = read.read(ch);
			}
			System.out.print(sb.toString());
			String a = sb.toString().replaceAll("@@@@@", ",");
			strs = a.substring(0, a.length() - 1);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strs;
	}

	// 用map保存一个类中的方法名和变量名
	public static Map<String, List<String>> saveMethodsOrFields(Constant c, Map<String, List<String>> map) {
		String s = c.getValue();
		int classStartIndex = s.lastIndexOf('/');
		int classEndIndex = s.indexOf('.');
		String key = s.substring(classStartIndex + 1, classEndIndex);

		List<String> methodList;
		if (map.containsKey(key)) {
			methodList = map.get(key);
		} else {
			methodList = new ArrayList<>();
		}

		int methodStartIndex = classEndIndex + 1;
		int methodEndIndex = s.length();
		String methodName = s.substring(methodStartIndex, methodEndIndex);
		methodList.add(methodName);

		map.put(key, methodList);

		return map;
	}

	public static void main(String[] args) {

		String filename = "add";
		String filePath = "C:\\workspace\\JvmTest\\src\\code\\" + filename + ".txt";
		readTxtFile(filePath);

		try {
			StringBuilder s = new StringBuilder();
			s.append("theory " + filename + "\n");
			s.append("imports" + "\n");
			s.append("  \"../Common/SystemClasses\"" + "\n");
			s.append("  JVMExec" + "\n");
			s.append("  \"~~/src/HOL/Library/Code_Target_Numeral\"" + "\n");
			s.append("begin" + "\n" + "\n");

			// 常量池的definition
			for (Entry<String, List<String>> entry : classAndMethodsMap.entrySet()) {
				if (!entry.getKey().equals("Object")) {
					if (entry.getKey().contains(":")) {
						int index = entry.getKey().indexOf(':');
						s.append(nameDef(entry.getKey().substring(0, index)));
					} else {
						s.append(nameDef(entry.getKey()));
					}
				}

				for (String method : entry.getValue()) {
					if (!method.contains("init")) {
						if (method.contains(":")) {
							int index = method.indexOf(':');
							s.append(nameDef(method.substring(0, index)));
						} else {
							s.append(nameDef(method));
						}
					}
				}
			}

			// 变量名的definition
			for (Entry<String, List<String>> fieldEntry : classAndfieldsMap.entrySet()) {
				for (String field : fieldEntry.getValue()) {
					String[] f = field.split(":");
					s.append(nameDef(f[0]));
				}
			}

			// ins的definition
			for (Entry<String, InsCode> insEntry : InscodeMap.entrySet()) {
				if (!classAndMethodsMap.containsKey(insEntry.getKey())) {
					s.append(InsDef(insEntry.getKey(), insEntry.getValue()));
				}
			}

			// 方法的definition
			for (Entry<String, List<String>> entry : classAndMethodsMap.entrySet()) {
				String cname = entry.getKey();
				s.append("definition " + cname + "_class" + "::" + "\"" + "jvm_method class" + "\"" + "\n");
				s.append("  where" + "\n");
				s.append("  \"" + cname + "_class" + " ==" + "\n");

				String supClass = "";
				for (Entry<String, InsCode> insEntry : InscodeMap.entrySet()) {
					if (insEntry.getKey().equals(cname)) {
						String[] codeList = insEntry.getValue().getCode().split("\n");
						String codeLine = codeList[1];
						if (codeLine.contains("Object")) {
							supClass = "Object";
							break;
						} else {
							int supStartIndex = codeLine.lastIndexOf('/');
							int supEndIndex = codeLine.indexOf('.', supStartIndex);
							supClass = codeLine.substring(supStartIndex, supEndIndex);
							break;
						}
					}
				}
				s.append("    (" + supClass + ", ");

				String fields = "[";
				for (Entry<String, List<String>> fEntry : classAndfieldsMap.entrySet()) {
					if (fEntry.getKey().equals(cname)) {
						for (String string : entry.getValue()) {
							String[] fs = string.split(":");
							fields = fields + "(" + fs[0] + ",";
							if (fs[1].equals("I")) {
								fields = fields + " Integer" + "),";
							} else if (fs[1].equals("Z")) {
								fields = fields + " Boolean" + "),";
							} else if (fs[1].contains(";")) {
								int typeStartIndex = fs[1].lastIndexOf('/');
								int tyepEndIndex = fs[1].length() - 1;
								String t = fs[1].substring(typeStartIndex + 1, tyepEndIndex);
								fields = fields + " Class " + t + "),";
							}

						}
					}
				}
				if (fields.length() > 2) {
					fields = fields.substring(0, fields.length() - 2) + "],\n";
				} else {
					fields = fields + "],\n";
				}
				s.append(fields);

				String methods = "[";
				List<String> methodList = entry.getValue();

				for (String m : methodList) {
					if (!methods.equals("[")) {
						methods = methods + ",";
					}
					String mName = "";
					if (!m.isEmpty() && !m.equals("")) {
						int mIndex = m.indexOf(':');
						mName = m.substring(0, mIndex);
						methods = methods + "(" + mName + "_name" + ", ";
					}
					for (Entry<String, InsCode> mEntry : InscodeMap.entrySet()) {
						if (m.equals(mEntry.getKey())) {
							InsCode mInsCode = mEntry.getValue();
							String mParams = "[";
							for (String p : mInsCode.getParams()) {
								mParams = mParams + p + ", ";
							}
							if (mParams.length() > 1) {
								mParams = mParams.substring(0, mParams.length() - 2);
							}
							mParams = mParams + "]";
							methods = methods + mParams + "," + mInsCode.getReturnType() + ", \n";

							String mbody = "(";
							mbody = mbody + mInsCode.getStack() + ", " + mInsCode.getLocals() + ", " + mName
									+ "_ins, [])";

							methods = methods + mbody + ")";

						}
					}
				}

				methods = methods + "]";

				s.append(methods + ")\"");

			}

			creatTxtFile(filename);
			writeTxtFile(s.toString());
			readDate();
			System.out.println("*********\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String nameDef(String realName) {
		String defName = realName + "_name";
		StringBuilder defbuilder = new StringBuilder();
		defbuilder.append("definition " + defName + " :: string" + "\n");
		defbuilder.append("  where" + "\n");
		defbuilder.append("\"" + defName + "==" + "\'\'" + realName + "\'\'\"" + "\n" + "\n");

		return defbuilder.toString();

	}

	public static String InsDef(String realName, InsCode ins) {
		String defName = realName + "_ins";
		StringBuilder defbuilder = new StringBuilder();
		defbuilder.append("definition " + defName + " :: bytecode" + "\n");
		defbuilder.append("  where" + "\n");
		defbuilder.append("  \"" + defName + "== [" + "\n" + ins.getCode() + "]\"" + "\n" + "\n");

		return defbuilder.toString();

	}

}
