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
						int num = Integer.parseInt(numString);
						constant.setNum(num);

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
							if (constant.getKey().equals("Methodref")) {
								saveMethodsOrFields(constant, classAndMethodsMap);
							} else if (constant.getKey().equals("Fieldref")) {
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
					}

					if (lineTxt.contains("stack=")) {
						int[] args = getInitStat(lineTxt);
						insCode.setStack(args[0]);
						insCode.setLocals(args[1]);
						insCode.setArgs_size(args[2]);
						isCodeBegin = true;
					}

					if (isCodeBegin && lineTxt.contains(":")) {
						code = code + lineTxt + "\n";
					}

					if ((lineTxt.equals("") || lineTxt.isEmpty()||lineTxt.contains("}")) && isCodeBegin) {
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
		int methodEndIndex = s.indexOf(':');
		String methodName = s.substring(methodStartIndex, methodEndIndex);
		methodList.add(methodName);

		map.put(key, methodList);

		return map;
	}

	public static void main(String[] args) {

		String filename = "add2";
		String filePath = "C:\\workspace\\JvmTest\\src\\code\\" + filename + ".txt";
		// "res/";
		readTxtFile(filePath);
		// List<String> classes = new ArrayList<>();

		// 读写txt测试
		// try {
		// for (int i = 0; i < 5; i++) {
		// creatTxtFile("test123");
		// writeTxtFile("显示的是追加的信息" + i);
		// String str = readDate();
		// System.out.println("*********\n" + str);
		// }
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// 单独读取类名
		// if (c.getKey().equals("Class")) {
		// String s = c.getValue();
		// int classStartIndex = s.lastIndexOf('/');
		// int classEndIndex = s.length();
		//
		// String classString = c.getValue().substring(classStartIndex + 1,
		// classEndIndex);
		//
		// classes.add(classString);
		// }
		// System.out.println("num=" + c.getNum() + ", key=" + c.getKey() +
		// ",
		// value=" + c.getValue());
		// }

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
					s.append(nameDef(entry.getKey()));
				}

				for (String method : entry.getValue()) {
					if (!method.contains("init")) {
						s.append(nameDef(method));
					}
				}
			}

			// 变量名的definition
			for (Entry<String, List<String>> fieldEntry : classAndfieldsMap.entrySet()) {
				for (String field : fieldEntry.getValue()) {
					s.append(nameDef(field));
				}
			}

			// ins的definition
			for (Entry<String, InsCode> insEntry : InscodeMap.entrySet()) {
				s.append(InsDef(insEntry.getKey(), insEntry.getValue()));
			}

			creatTxtFile(filename);
			writeTxtFile(s.toString());
			String str = readDate();
			System.out.println("*********\n" + str);
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
