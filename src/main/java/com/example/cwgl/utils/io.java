package com.example.cwgl.utils;

import java.io.PrintStream;
import java.util.*;


public class io {
	
	private static final Scanner in = new Scanner(System.in);
	private static final PrintStream out = System.out;
	private static final io this_ = new io();
	
	private io() {
		
	}
	
	public static io pm(String name, Object o) {
		print(name,o);
		return this_;
	}
	public static io p(String name, Object o) {
		print(name,o);
		return this_;
	}
	public static io p(Object o) {
		print(o);
		return this_;
	}
	
	//输出数组
	public static void printArray(String name,Object[] arr,int tabNum) {
		out.println(name==null?"Arr:":name+" :");
		StringBuilder tab = new StringBuilder("    ");
		for (int i = 0; i < tabNum; i++) 
			tab.append("    ");
		out.print(tab);
		out.println(Arrays.toString(arr));
		
	}
	//输出Map集合
	public static void printMap(String name,Map<?, ?> map,int tabNum) {
		out.println(name==null?"Map:":name+" :");
		StringBuilder tab = new StringBuilder("    ");
		for (int i = 0; i < tabNum; i++) 
			tab.append("    ");
		Set<?> keySet = map.keySet();
		for (Object o : keySet) {
			out.print(tab);
			out.print(o+" : ");
			print(map.get(o),tabNum);
		}
	}
	//输出List集合
	public static void printList(String name,List<?> list,int tabNum) {
		out.println(name==null?"List:":name+" :");
		StringBuilder tab = new StringBuilder("    ");
		for (int i = 0; i < tabNum; i++) 
			tab.append("    ");
		int len = list.size();
		for (int i = 0; i < len; i++) {
			out.print(tab);
			out.print("["+i+"]"+" : ");
			print(list.get(i),tabNum);
		}
	}
	public static io title(String title) {
		title="<"+title+">";
		int l = (40-title.length())/2;
		int i=0;
		for (; i < l; i++) 
			out.print("=");
		i+=title.length();
		out.print(title);
		for (; i < 40; i++) 
			out.print("=");
		print();
		return this_;
	}
	//输出==线
	public static io line() {
		line(40);
		return this_;
	}
	//输出指定长度==线
	public static void line(int num) {
		printLine("=", num);
	}
	/**
	 * 输出指定字符长度的分割线
	 * @param str
	 * @param num
	 */
	public static void printLine(String str,int num) {
		print();
		for (int i = 0; i < num; i++) {
			out.print(str);
		}print();
	}
	//输出空行
	public static void print() {
		out.println();
	}
	/**
	 * 智能打印一个对象
	 * @param o
	 */
	public static void print(Object o) {
		print(null,o,-1);
	}
	//name : value 形式打印
	public static void print(String name,Object o) {
		print(name, o, -1);
	}
	public static void print(Object o,int tabNum) {
		print(null,o,tabNum);
	}
	/**
	 * 智能分级展开打印一个对象
	 * @param name
	 * @param o
	 * @param tabNum
	 */
	public static void print(String name,Object o,int tabNum) {
		if(o==null) {
			out.println("null");
			return;
			};
		if(o instanceof List) {
			printList(name,(List<?>) o, ++tabNum);
			return;
		}if(o instanceof Map) {
			printMap(name,(Map<?, ?>) o,++tabNum);
			return;
		}if(o.getClass().isArray()) {
			printArray(name,(Object[]) o,tabNum);
			return;
		}
		out.print((tabNum==-1?"= ":"")+(name==null?"":name+" : ")+o+"\n");
	}

	/**
	 * 从控制台拿到一个序号有上限，自动-1
	 * 输入序号不能小于等于0
	 * @param max 序号上限
	 * @return 返回结果自动-1
	 */
	public static int index(int max) {
		int index = 0;
		while(true)
		try {
			out.print(">> ");
			index = in.nextInt();
			if(index<=0) {
				err("序号不能小于等于0");
				continue;
			}
			if(index>max && max!=-1) {
				err("序号不能大于"+max);
				continue;
			}
			break;
		} catch (InputMismatchException e) {
			String error = in.next();
			err("输入错误",error);
		}
		return index-1;
	}
	/**
	 * 拿到一个序号无上限
	 * @return 返回序号-1
	 */
	public static int index() {
		return index(-1);
	}
	/**
	 * 拿到一个int值
	 * @return 返回该int值
	 */
	public static int int_() {
		int index = 0;
		while(true)
		try {
			out.print(">> ");
			index = in.nextInt();
			break;
		} catch (InputMismatchException e) {
			String error = in.next();
			err("输入错误",error);
		}
		return index;
	}
	/**
	 * 拿到一个double值
	 * @return 返回该double值
	 */
	public static double double_() {
		double doub = 0;
		while(true)
		try {
			out.print(">> ");
			doub = in.nextDouble();
			break;
		} catch (InputMismatchException e) {
			String error = in.next();
			err("输入错误",error);
		}
		return doub;
	}
	/**
	 * 拿到一个字符串值
	 */
	public static void get() {
		out.print(">> ");
		in.next();
	}
	/**
	 * 暂停，输入任意字符继续
	 */
	public static void pause() {
		out.println("--输入任意字符继续...");
		get();
	}
	/**
	 * 判断是否输入的 q 或 Q 
	 * @param str
	 * @return
	 */
	public static boolean esc(String str) {
		return str.equals("q") || str.equals("Q");
	}
	public static void err(String msg) {
		err(msg,"");
	}
	public static void err(String msg,String error) {
		out.println("!! "+msg+error);
	}
	
}
