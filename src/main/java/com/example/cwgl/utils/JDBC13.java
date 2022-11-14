package com.example.cwgl.utils;

import com.example.cwgl.entity.Table;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.ExecutorException;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.property.PropertyTokenizer;
import org.apache.ibatis.scripting.xmltags.ForEachSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import javax.sql.DataSource;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Date;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 1、引入通过mapper。xml的sqlId进行CRUD
 * 2、优化分页方式
 * 3、批量导入
 * 4、插入自动设置创建时间更新时间
 * 5、添加 in (?..)条件 或者 ,sex in
 * 6、优化toMap（包括父级）
 * 7、添加等号对象条件查询，查询一个
 * 8、删除updateById()
 * 9、解决子类取值错误
 * 10、解决多线程分页耦合
 * 11、删除事务操作
 * 12、添加日志打印
 * 13、新增树结构查询
 * 14、优化setSql 改为SB
 * @author 58347
 * @version 12
 *
 */

/**
 @Bean
 public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) throws IOException {
	 SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
	 org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
	 configuration.setMapUnderscoreToCamelCase(true);
	 configuration.setCallSettersOnNulls(true);
	 sqlSessionFactoryBean.setConfiguration(configuration);
	 sqlSessionFactoryBean.setDataSource(dataSource);
	 ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
	 sqlSessionFactoryBean.setMapperLocations(resolver.getResources(mapperLocations));
	 return sqlSessionFactoryBean;
 }

 @Bean
 public JDBC12 jdbc12(DataSource dataSource, SqlSessionFactoryBean sqlSessionFactoryBean) throws Exception {
	 JDBC12.setDs(dataSource);
	 JDBC12.setSqlSessionFactory(sqlSessionFactoryBean.getObject());
	 JDBC12.setEntityPackage(Table.entityPackage);
	 // JDBC12.setAutoToLine(false);
	 return new JDBC12();
 }
 */

/**
 * JDBC工具
 * 注意：必须设置的参数
 *
 * 基本操作
 * --ds：数据源
 * --entityPackage：实体类所在包位置
 * 还可以配置 “sql-config.properties” 
 * sql映射配置文件，来应对复杂sql
 * 
 * mapper操作
 * --mapperPackage：mapper文件所在位置
 * --sqlSessionFactory：sqlSession工厂
 *
 * @author lcc
 * @date 2022/3/18 15:08
 */
public class JDBC13 {

	public static void main(String[] args) throws ClassNotFoundException {
		Utils.createTable("H:\\bs\\cwgl\\src\\main\\java\\com\\example\\cwgl\\entity",null);

		Utils.setFilePath("H:\\bs\\news\\src\\main\\resources\\mapper");
		String[][] table = {
//			new String[]{"userId", 		Table.User},
//			new String[]{"adminId",		Table.Admin},
//			new String[]{"collectionId",Table.Collection},
//			new String[]{"commId", 		Table.Comm},
//			new String[]{"commcollectionId", Table.Commcollection},
//			new String[]{"newsId", 		Table.News},
//			new String[]{"sensitiveId", Table.Sensitives},
//			new String[]{"swiperId", 	Table.Swiper},
//			new String[]{"typeId", 		Table.Type},
//			new String[]{"watchId", 	Table.Watch}
				new String[]{"id", 			"Menu"}
		};
		for (String[] strings : table) {
			Utils.createMapper(
					Class.forName(Table.entityPackage +"."+ strings[1]),
					strings[1],
					strings[0],
					strings[1] + "表",
					Table.mapperPackage +"."+ strings[1]);
		}
	}


	private int 					one 	  = 1;			//第几层一对多的查询
	private int						threadNum = 0;			//线程数
	private String 					oneMoreConditon;		//进行一对多的条件，一的列名
	private String 					moreTable;				//一对多，多的表名
	private String 					moreSql;				//一对多，多的sql语句
	private boolean 				startOneMore = false;	//开启一对多
	private Connection 				con;					//数据库连接对象，连接池

	private static String 			entityPackage;			//实体类所在包位置
	private static String 			mapperPackage;			//mapper的根namespace
	private static boolean 			autoToHump 	 = true;	//查询结果自动转为驼峰命名
	private static boolean 			autoToLine 	 = true;	//对象字段自动转为下划线
	private static DataSource 		ds;						//数据源
	private static Properties 		sqlconfig;				//sql配置文件
	private static SqlSessionFactory sqlSessionFactory;		// sqlSession工厂
	private final Map<Object, Object> 	moreCache 			//一对多，多的缓存
			= new HashMap<>();

	/**
	 * 查询一个
	 * @param table 表名
	 * @param sqlId mapper文件里的sql id
	 * @param parameter 参数
	 * @return
	 */
	public Object mGet(final String table, final String sqlId, final Object parameter) {
		return execute(session -> session.selectOne(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	/**
	 * 查询列表
	 * @param table 表名
	 * @param sqlId mapper文件里的sql id
	 * @param parameter 参数
	 * @return
	 */
	public List<?> mQuery(final String table, final String sqlId, final Object parameter) {
		return  (List<?>) execute(session -> session.selectList(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	public int mDelete(final String table, final String sqlId, final Object parameter) {
		return (Integer) execute(session -> session.delete(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	public int mUpdate(final String table, final String sqlId, final Object parameter) {
		return (Integer) execute(session -> session.update(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	public int mInsert(final String table, final String sqlId, final Object parameter) {
		return (Integer) execute(session -> session.insert(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	public Object mExecute(final String table, final String sqlId, final Object parameter) {
		return execute(session -> session.selectOne(mapperPackage+"."+table+"."+sqlId, parameter));
	}

	/**
	 * 封装好了的通用ibatis分页查询方法，只要配置好了Mapper文件，可进行任何分页查询
	 *
	 * @param table 表名
	 * @param sqlId sqlId
	 * @param paramMap  一般是由页面来的查询参数
	 * @param curPage   当前页码
	 * @param pageSize  每页记录数
	 * @return
	 */
	public Map<String, Object> mQueryPage(String table, String sqlId, Object paramMap, int curPage, int pageSize) {
		String statement = mapperPackage+"."+table+"."+sqlId;
		try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
			List<?> resultList = sqlSession.selectList(statement, paramMap, new RowBounds(pageSize * (curPage - 1), pageSize));
			return getPageMap(resultList, getTotalCount(sqlSession, statement, paramMap), curPage, pageSize);
		}
	}

	/**
	 * 查询树结构
	 * @param table		表名
	 * @param condition	条件
	 * @param idName	id自动名
	 * @param pidName	pid字段名
	 * @param <T>
	 * @return
	 */
	public <T> List<T> selTree(String table,String condition,String idName,String pidName,Object...params){
		startOneMore(table,"SELECT * FROM "+table+" WHERE "+pidName+"=?",idName);
		return selBC(table,condition,params);
	}

	/**
	 * 插入一个对象，
	 * @param table	那个表
	 * @param obj	对象
	 * @return int
	 */
	public int insert(String table, Object obj, String idName) {
		Map<String,Object> map = Utils.toMap(obj,getClass(table),true);
		StringBuilder sql = new StringBuilder("INSERT INTO "+table+" SET ");
		Object[] params = new Object[map.size()];
		addSet(sql, map, params);
		int r= exeUpdate(sql.toString(), params);
		if(idName != null) {
			setObjAutoId(obj, idName);
		}
		return r;
	}

	/**
	 * 插入一个list，
	 * @param table	那个表
	 * @param list	对象
	 * @return int
	 */
	public int insertList(String table, List list) {
		if(list == null || list.size() == 0){
			return 0;
		}
		StringBuilder sql = new StringBuilder("INSERT INTO "+table+"(");
		Map<String, Object> map1 = Utils.toMap(list.get(0),getClass(table),true);
		if(map1.size() == 0){
			return 0;
		}
		Object[] params = new Object[map1.size() * list.size()];
		map1.keySet().forEach(key -> sql.append(getColumnNames(key)).append(", "));
		sql.deleteCharAt(sql.length()-1).setCharAt(sql.length()-1,')');
		sql.append(" VALUES");
		int i = 0;
		for(Object obj:list) {
			Map<String, Object> map = i==0?map1: Utils.toMap(obj,getClass(table),true);
			sql.append(" (");
			for(String key:map.keySet()){
				sql.append("?, ");
				params[i++]=map.get(key);
			}
			sql.deleteCharAt(sql.length()-1).setCharAt(sql.length()-1,')');
			sql.append(",");
		}
		sql.deleteCharAt(sql.length()-1);
		return exeUpdate(sql.toString(), params);
	}

	/**
	 * 通过条件数组数组 批量删除
	 * @param table	表
	 * @param cName	条件属性名
	 * @param arr	数组
	 * @return	int
	 */
	public int deletes(String table, String cName,Object[] arr) {
		StringBuilder sb = new StringBuilder("DELETE FROM "+table+" WHERE "+cName+" in(");
		for (int i = 0; i < arr.length; i++)
			sb.append("?,");
		sb.setCharAt(sb.length()-1, ')');
		return exeUpdate(sb.toString(), arr);

	}

	/**
	 * 通过表删除，参数先用?站位
	 * 然后把要传入的参数写到参数数组
	 * @param table		表
	 * @param condition	条件
	 * @param params	参数
	 * @return	int
	 */
	public int delete(String table, String condition,Object... params) {
		return exeUpdate("DELETE FROM " + table +" WHERE "+condition, params);
	}

	public int deleteById(String table, String idName, Object param){
		return delete(table,idName+"=?",param);
	}

	/**
	 * 通过对象自动确定修改
	 * @param table	那个表
	 * @param obj	存储修改信息的对象
	 * @param cName	用于确定条件字段名
	 * @return int
	 */
	public int update(String table, Object obj, String cName) {
		Map<String, Object> map = Utils.toMap(obj,getClass(table),false);
		StringBuilder sql = new StringBuilder("UPDATE "+table+" SET ");
		cName = getColumnNames(cName);
		Object[] params = new Object[map.size()];
		//先拿出条件字段
		params[map.size()-1]=map.remove(cName);
		addSet(sql, map, params);
		sql.append("WHERE ").append(cName).append("=?");
		return exeUpdate(sql.toString(), params);

	}

	/**
	 * 通过表名修改，参数先用?站位
	 * 最后写在参数数组里
	 * @param table	 那个表
	 * @param set	 修改啥
	 * @param where	 修改谁
	 * @param params 参数数组
	 * @return int
	 */
	public int update(String table, String set, String where, Object... params) {
		if(null != Utils.getField(getClass(table),"updateTime")){
			set = getColumnNames("updateTime")+"=now()," + set;
		}
		return exeUpdate("UPDATE "+table+" SET "+set+" WHERE "+where, params);
	}

	/**
	 * 查询的结果存入map 第一列为key，第二列为value
	 * @param sql
	 * @param params
	 * @return
	 */
	public Map<String,Object> selMap(String sql,Object... params){
		PreparedStatement pstatement = setSql(sql, params).preparedStatement;
		Map<String,Object> map = new HashMap<>();
		ResultSet resultset = null;
		try {
			resultset = pstatement.executeQuery();
			while(resultset.next()) {
				map.put(resultset.getObject(1).toString(),
						resultset.getObject(2));
			}
			return map;
		} catch (SQLException e) {
			throw new JDBCException(e);
		} finally {
			close(resultset,pstatement);
		}
	}

	/**
	 * 返回一个Map套数组
	 * @param cNames	列名
	 * @param sql		sql
	 * @param params	参数
	 * @return			Map<String,List<Object>>
	 */
	public Map<String,List<Object>> selMapList(String cNames,String sql,Object... params){
		PreparedStatement pstatement = setSql(sql, params).preparedStatement;
		Map<String,List<Object>> map = new HashMap<>();
		ResultSet resultset = null;
		try {
			resultset = pstatement.executeQuery();
			ResultSetMetaData meta = resultset.getMetaData();
			String [] columnName = getColumnNames(cNames,meta);
			for (String value : columnName) map.put(value, new ArrayList<>());
			while(resultset.next()) {
				for (String s : columnName) {
					map.get(s).add(resultset.getObject(s));
				}
			}
			return map;
		} catch (SQLException e) {
			throw new JDBCException(e);
		} finally {
			close(resultset,pstatement);
		}
	}

	/**
	 * 查询指定字段用List<Map>接收
	 * 不用创建对应的实体类
	 * 自动生成驼峰映射
	 * cNames : id, name, stu_no 指定字段， * 全部字段
	 * @return 	List<Map<String,Object>>
	 */
	public List<Map<String,Object>> selListMap(String cNames, String table, String condition, Object... params){
		return selListMapSql(cNames,"SELECT "+cNames+" FROM "+table+" where "+condition,params);
	}

	public Map<String,Object> pageSelListMap(String cNames, String table, String condition, int curPage, int pageSize, Object... params){
		return pageSelListMapSql(cNames,"SELECT "+cNames+" FROM "+table+" where "+condition,curPage,pageSize,params);
	}

	/**
	 * 查询ListMap 根据sql
	 * @param cNames
	 * @param sql
	 * @param params
	 * @return
	 */
	public List<Map<String,Object>> selListMapSql(String cNames, String sql, Object... params){
		return (List<Map<String, Object>>) pageSelListMapSql(cNames,sql,-1,0,params).get("list");
	}

	public Map<String,Object> pageSelListMapSql(String cNames, String sql, int curPage, int pageSize, Object... params){

		MyParam myParam = setSql(sql, params,curPage,pageSize);
		PreparedStatement pstatement = myParam.preparedStatement;
		List<Map<String, Object>> lmap = new ArrayList<>();
		ResultSet resultset=null;

		try {
			resultset = pstatement.executeQuery();
			ResultSetMetaData meta = resultset.getMetaData();
			String [] columnName = getColumnNames(cNames,meta);
			String [] columnName2 = null;//驼峰命名数组
			int count=columnName.length;
			if(autoToHump) {
				columnName2 = new String[columnName.length];
				for (int i = 0; i < columnName2.length; i++) {
					columnName2[i]= Utils.toHump(columnName[i]);
				}
			}
			while(resultset.next()) {
				Map<String, Object> map = new HashMap<>();
				//把这一行放到map
				for (int i = 0; i < count; i++) {
					Object temp = resultset.getObject(columnName[i]);
					map.put(columnName2==null?columnName[i]:columnName2[i],
							temp==null?"":temp);
				}
				lmap.add(map);
			}
		} catch (SQLException e) {
			throw new JDBCException(e);
		} finally {
			close(resultset,pstatement);
		}
		return getPageMap(lmap,myParam.count,curPage,pageSize);
	}

	/**
	 * 等号对象条件查询
	 * @param table
	 * @param obj
	 * @param <T>
	 * @return
	 */
	public <T> List<T> selEq(String table, Object obj){
		String sql = "SELECT * FROM "+table;
		StringBuilder sb = new StringBuilder();
		Map<String, Object> map = Utils.toMap(obj);
		List<Object> params = new ArrayList<>();
		map.keySet().forEach(key ->{
			Object val = map.get(key);
			if(val != null && !val.equals("")){
				sb.append(getColumnNames(key)).append("=? and ");
				params.add(val);
			}
		});
		if(!sb.toString().trim().equals("")){
			sb.delete(sb.length()-5,sb.length());
			sql = sql + " WHERE "+sb;
		}
		return sel(table,sql,params.toArray());
	}

	/**
	 * 等号对象条件查询一个
	 * @param table
	 * @param obj
	 * @param <T>
	 * @return
	 */
	public <T> T selOneEq(String table, Object obj){
		List<T> list = selEq(table,obj);
		if(list.size()>1){
			throw new JDBCException("查出了多个值");
		}
		return list.size()==0?null:list.get(0);
	}

	/**
	 * 查询一条记录
	 * @param <T>		对象
	 * @param table		那个表
	 * @param condition 条件
	 * @param params	?参数
	 * @return T		对象类型
	 */
	public <T> T selOneObj(String table, String condition,Object... params) {
		List<T> list = sel(table, "SELECT * FROM "+table + " WHERE "+condition, params);
		if(list.size()>1){
			throw new JDBCException("查出了多个值");
		}
		return list.size()==0?null:list.get(0);
	}

	/**
	 * 查询一条记录 返回map形式
	 * @param cNames	列名
	 * @param table		表名
	 * @param condition	条件
	 * @param params	参数
	 * @return			Map<String,Object>
	 */
	public Map<String,Object> selOneMap(String cNames,String table, String condition,Object... params){
		List<Map<String,Object>> lmap =  selListMapSql(cNames, "SELECT "+cNames+" FROM "+table + " WHERE "+condition, params);
		if(lmap.size()>1){
			throw new JDBCException("查出了多个值");
		}
		return lmap.size()==0?null:lmap.get(0);
	}

	/**
	 * 查询一个基本类型
	 * @param cName		列名
	 * @param table		表名
	 * @param condition	条件
	 * @param params	参数
	 * @return			Map<String,Object>
	 */
	public <T> T selOneBC(String cName,String table, String condition,Object... params) {
		return selOne(cName, "SELECT "+cName+" FROM "+table + " WHERE "+condition, params);
	}

	/**
	 * 获得一个T类型的数据
	 * @param <T>	基本类型
	 * @param sql	sql
	 * @param params 参数
	 * @return T	基本类型 或 对象类型
	 */
	@SuppressWarnings("unchecked")
	public <T> T selOne(String cName, String sql, Object... params) {
		PreparedStatement pstatement = setSql(sql, params).preparedStatement;
		ResultSet resultset=null;
		try {
			resultset = pstatement.executeQuery();
			if(!resultset.next())
				return null;
			ResultSetMetaData metaData = resultset.getMetaData();
			int count = metaData.getColumnCount();
			if(count==1 || cName.equals("1"))
				return (T) resultset.getObject(1);
			return (T) resultset.getObject(cName);

		} catch (SQLException e) {
			throw new JDBCException(e);
		}finally {
			close(resultset,pstatement);
		}
	}

	/**
	 * 查找整个表格
	 * @param <T>	对象类型
	 * @param table 表格
	 * @return 		List
	 */
	public <T> List<T> sel(String table){
		return sel(table, "SELECT * FROM "+table);
	}
	public Map<String,Object> pageSel(String table, int curPage, int pageSize){
		return pageSel(table, "SELECT * FROM "+table,curPage,pageSize);
	}
	/**
	 * 条件查找表格
	 * @param <T>		对象类型
	 * @param table		表格
	 * @param condition 条件
	 * @param params	?占位符的值
	 * @return	List
	 */
	public <T> List<T> selBC(String table, String condition, Object...params ){
		String sql = "SELECT * FROM "+table;
		if(condition==null || condition.isEmpty())
			return sel(table, sql);
		sql += " WHERE "+condition;
		return sel(table, sql,params);

	}

	public Map<String,Object> pageSelBC(String table, String condition, int curPage,int pageSize,Object...params ){
		String sql = "SELECT * FROM "+table;
		if(condition==null || condition.isEmpty())
			return pageSel(table, sql,curPage,pageSize);
		sql += " WHERE "+condition;
		return pageSel(table, sql,curPage,pageSize,params);

	}

	/**
	 * 通过对象自动匹配条件查询
	 * @param cNames	要查询的列
	 * @param table		那个表
	 * @param obj		条件对象
	 * @param operators	运算符: =,,,like,,<br>
	 * 			还可以是 1~9的数字代表跳过几个 如 =,2,like,2 同上面条件<br>
	 * 		特殊条件：is null, is not null, ='' 直接写就行<br>
	 * 		注意：类中最后有效字段之前都要对应运算符，且按照顺序<br>
	 * @return List<Map<String,Object>>
	 */
	public List<Map<String,Object>> selListMapByObj(String cNames, String table, Object obj, String operators){
		StringBuilder sb = new StringBuilder("SELECT "+cNames+" FROM "+table);
		List<Object> params;
		params = setWhere(table,obj,sb,operators);
		return selListMapSql(cNames, sb.toString(), params.toArray());
	}

	public Map<String,Object> pageSelListMapByObj(String cNames, String table, Object obj,int curPage,int pageSize, String operators){
		StringBuilder sb = new StringBuilder("SELECT "+cNames+" FROM "+table);
		List<Object> params;
		params = setWhere(table,obj,sb,operators);
		return pageSelListMapSql(cNames, sb.toString(),curPage,pageSize, params.toArray());
	}

	/**
	 * 通过对象自动匹配条件查询
	 * @param table		那个表
	 * @param obj		条件对象
	 * @param operators	运算符: =,,,like,,<br>
	 * 			还可以是 1~9的数字代表跳过几个 如 =,2,like,2 同上面条件<br>
	 * 		特殊条件：is null, is not null, ='' 直接写就行<br>
	 * 		注意：类中最后有效字段之前都要对应运算符，且按照顺序<br>
	 * @return List<Map<String,Object>>
	 */
	public <T> List<T> selByObj(String table, Object obj, String operators) {
		StringBuilder sb = new StringBuilder("SELECT * FROM "+table);
		List<Object> params = setWhere(table,obj,sb,operators);
		return sel(table, sb.toString(), params.toArray());
	}

	public Map<String, Object> pageSelByObj(String table, Object obj, int curPage, int pageSize, String operators) {
		StringBuilder sb = new StringBuilder("SELECT * FROM "+table);
		List<Object> params;
		params = setWhere(table,obj,sb,operators);
		return pageSel(table, sb.toString(),curPage,pageSize, params.toArray());
	}

	public <T> List<T> sel(String table, String sql , Object... params){
		return (List<T>) pageSel(table,sql,-1,0,params).get("list");
	}

	/**
	 * 查询
	 * @param table		查询那个表
	 * @param sql		拼的sql或者sqlkey
	 * @param params	设置占位符的值
	 * @return			List
	 */
	@SuppressWarnings("unchecked")
	public Map<String,Object> pageSel(String table, String sql , int curPage, int pageSize, Object... params){

		int one = this.one++;
		MyParam myParam = setSql(sql, params,curPage,pageSize);
		PreparedStatement pstatement = myParam.preparedStatement;
		List<Object> list = new ArrayList<>();
		ResultSet resultset=null;
		try {
			Class<?> c = Class.forName(entityPackage + "." + Utils.toHump(table));
			Field[] fields = c.getDeclaredFields();
			resultset = pstatement.executeQuery();
			if(startOneMore && moreTable==null)		//进行不分开一对多查询
				fieldsSetObjects(fields, resultset, list, c);
			else
			while(resultset.next()) {
				Object obj = c.newInstance();
				fieldsSetObject(fields, obj, resultset,-1);
				list.add(obj);
			}
			if(one==1) {
				this.moreTable=null;
				startOneMore=false;
				moreCache.clear();
				this.one=1;
			}
		} catch (IllegalArgumentException | IllegalAccessException |
				SQLException | InstantiationException | ClassNotFoundException e) {
			throw new JDBCException(e);
		}finally {
			close(resultset,pstatement);
		}
		return getPageMap(list,myParam.count,curPage,pageSize);
	}

	/**
	 * 开启一对多查询
	 * @param table		多的表名
	 * @param sql		sql
	 * @param conditionName	在 一 中的条件字段名字
	 */
	public void startOneMore(String table,String sql,String conditionName) {
		this.moreTable=table;
		this.moreSql=sql;
		this.oneMoreConditon=conditionName;
		this.startOneMore=true;
	}

	/**
	 * 开启一对多查询
	 * @param table			多的表名
	 * @param condition		where 条件
	 * @param conditionName	在 一 中的条件字段名字
	 */
	public void startOneMoreBC(String table,String condition,String conditionName) {
		startOneMore(table, "SELECT * FROM "+table+" WHERE "+condition, conditionName);
	}

	/** 开启一对多不分开查询 */
	public void startOneMore() {
		this.startOneMore=true;
	}

	// 执行一条更新类语句
	public int exeUpdate(String sql,Object... params) {
		PreparedStatement pstatement = setSql(sql, params).preparedStatement;
		try {
			return pstatement.executeUpdate();
		} catch (SQLException e) {
			throw new JDBCException(e);
		}finally {
			close();
		}

	}

	private Object execute(SqlSessionCallback action) {
		if (mapperPackage == null) {
			throw new JDBCException("未配置mapperPackage");
		}
		SqlSession session = null;
		try {
			session = sqlSessionFactory.openSession();
			return action.doInSession(session);
		} finally {
			if (session != null) {
				session.commit();
				session.close();
			}
		}
	}

	//关闭连接对象，执行对象，结果对象
	private void close(ResultSet resultset,	 PreparedStatement pstatement) {
		if(resultset!=null)
			try {
				resultset.close();
			} catch (SQLException e) {
				logInfo("结果集关闭异常");
				e.printStackTrace();
			}
		if(pstatement!=null)
			try {
				pstatement.close();
			} catch (SQLException e) {
				logInfo("执行对象关闭异常");
				e.printStackTrace();
			}
		if(con!=null && --threadNum==0 && !startOneMore)
			try {
				con.close();
			} catch (SQLException e) {
				logInfo("连接对象关闭异常");
				e.printStackTrace();
			}finally {
				con=null;
			}
	}
	private void close(){
		close(null,null);
	}

	//给对象字段设置值
	private Field fieldsSetObject(Field[] fields , Object obj,ResultSet resultset,int oneLen)
			throws IllegalArgumentException, IllegalAccessException, SQLException {

		Field moreField = null;
		Integer id = null;
		for (int i=0; i < fields.length; i++) {
			fields[i].setAccessible(true);
			//设置一对多字段
			if(startOneMore && fields[i].getType()==List.class) {
				if(moreTable!=null) //一对多分开查询
					fields[i].set(obj, sel(moreTable, moreSql, resultset.getObject(oneMoreConditon)));
				else 				//一对多不分开查询，返回一种多的那个list字段
					moreField=fields[i];
				continue;
			}
			Object value = null;
			//普通赋值，根据名字拿值
			if(oneLen==-1) {
				String column=fields[i].getName();
				try {
					//SQLException,说明字段没找到，那就转下划线试试,再没有就算了
					value = resultset.getObject(column);}				catch(SQLException e) {try {
					value = resultset.getObject(Utils.toLine(column));}	catch(SQLException ignored) {}
				}
			}else {
				//不分开一对多时，根据下标拿值，一的结束后，剩下都是多的字段
				value = resultset.getObject(oneLen+i);
				if(i==0) id=resultset.getInt(oneLen);
			}
			if(value==null) {
				if(fields[i].getType()==String.class)
					value="";
			}
			fields[i].set(obj, value);//IllegalArgumentException | IllegalAccessException e
		}
		if(oneLen!=-1 && id!=null)
			moreCache.put(id, obj);
		return moreField;
	}

	//设置一对多字段（不分开）
	@SuppressWarnings("unchecked")
	private <T> void fieldsSetObjects(Field[] fields, ResultSet resultset, List<T> list, Class<?> c)
			throws InstantiationException, IllegalAccessException, SQLException
	{
		boolean b = resultset.next();
		if(!b) return;

		Object id;
		while (b) {
			id = resultset.getObject(1);            //记录当前一 的id
			Object obj = c.newInstance();
			Field mf = fieldsSetObject(fields, obj, resultset, -1);
			Type genericType = mf.getGenericType();                //获得字段类型
			ParameterizedType pt = (ParameterizedType) genericType;    //转成泛型类型
			Class<?> mc = (Class<?>) pt.getActualTypeArguments()[0];    //得到泛型里的class类型对象

			Field[] mfs = mc.getDeclaredFields();
			mf.set(obj,new ArrayList<>());
			List<Object> mlist = (List<Object>) mf.get(obj);
			//设置一的list属性
			while (true) {
				Object mobj;
				Object mId = resultset.getObject(fields.length);
				if (moreCache.containsKey(mId))     //如果缓存有就从缓存拿
					mobj = moreCache.get(mId);
				else {
					mobj = mc.newInstance();        //缓存没有从新创建
					fieldsSetObject(mfs, mobj, resultset, fields.length);
				}
				mlist.add(mobj);

				b = resultset.next();
				if (!b) break;
				Object id2 = resultset.getObject(1);
				if (!id.equals(id2)) break;    //当前一 的id 和下一条记录一的id比较，不相等才走下一个 一
			}
			list.add((T) obj);
		}
	}

	private MyParam setSql(String sql,Object[] params) {
		return setSql(sql,params,-1,0);
	}
	//给 pstatement 的占位符设置参数,先获得 con ， 赋值pstatement
	private MyParam setSql(String sql,Object[] params,int curPage,int pageSize) {

		con=getCon();
		MyParam myParam;
		PreparedStatement pstatement = null;
		if(!sql.contains(" "))
			sql=sqlconfig.getProperty(sql);

		StringBuilder sbSql = new StringBuilder(sql);
		//如果开启分页，要去掉 为null和"" 的条件
		if(params==null) params=new Object[0];
		params = removeEmpty(sbSql,params);
		params = setIn(sbSql,params);

		int count = Utils.countStr(sbSql.toString(), "\\?");
		if(count != params.length){
			throw new JDBCException("参数数量不匹配；占位符"+count+"个，参数"+params.length+"个");
		}
		myParam = curPage==-1
				? new MyParam(sbSql.toString(), params)
				: new MyParam(selOne("1", getCountSql(sbSql), params), params, curPage, pageSize,sbSql.append(" LIMIT ?,?").toString());
		try {
			pstatement = con.prepareStatement(myParam.sql);
			myParam.preparedStatement=pstatement;
			for (int i = 0; i < params.length; i++) {
				pstatement.setObject(i+1, params[i]);
			}
			if(curPage!=-1){
				pstatement.setInt(params.length+1, (curPage-1)*10);
				pstatement.setInt(params.length+2, pageSize);
			}
		} catch (SQLException e) {
			throw new JDBCException(e);
		}finally {
			assert pstatement != null;
			logSql(pstatement,sql,params,curPage,pageSize);
		}
		return myParam;
	}

	private static String getCountSql(StringBuilder sql){
		int i = sql.indexOf(" FROM ");
		if(i==-1){
			i=sql.indexOf(" from ");
		}
		if(i==-1){
			throw new JDBCException("sql有误，未找到from");
		}
		return "SELECT count(*) "+sql.substring(i);
	}

	//分页查询时的为 null和"" 的条件
	private Object[] removeEmpty(StringBuilder sql, Object[] params) {

		int len = params.length;
		int size = 0;
		//计算新参数数组的长度
		for (Object param : params)
			if (param != null && !param.equals(""))
				size++;
		Object[] params2 = new Object[size];
		//排除默认条件后 得到有效and是从第几个开始的。 0：where，1：第一个and
		int startSubAnd = Utils.countStr(sql.toString(), " AND ")-len+1;
		int j=0;//参数数组2的下标

		if(size==0 && startSubAnd==0) {
			sql.substring(0, Utils.getStartIndex(sql, " WHERE "));
			return params2;
		}
		for (int i = 0; i < len; i++) {
			if(params[i]==null || params[i].equals("")) {
				if(j==0 && startSubAnd==0) 	//开始位置
					sql.delete(Utils.getStartIndex(sql," WHERE ")+6, Utils.getStartIndex(sql," AND ")+4);
				else if(i!=len-1) 			//中间位置
					sql.delete(Utils.getIndexOf(sql, startSubAnd," AND "), Utils.getIndexOf(sql, startSubAnd+1," AND "));
				else 						//结束位置
					sql.delete(Utils.getIndexOf(sql, startSubAnd," AND "), sql.length());
			}else {
				params2[j++]=params[i];
				startSubAnd++;
			}
		}
		return params2;
	}

	private static Map<String, Object> getPageMap(List<?> list, Integer count, int curPage, int pageSize) {
		Map<String, Object> map = new HashMap<>();
		if(count==null){
			map.put("list",list);
			return map;
		}
		int maxPage = (int) Math.ceil(count * 1.0 / pageSize);
		map.put("count", count);    	//总记录条数
		map.put("list", list);        	//查询结果
		map.put("maxPage", maxPage);    //最大页码
		map.put("pageSize", pageSize);  //每页大小
		map.put("curPage", curPage);    //当前页码
		map.put("nextPage", curPage == maxPage ? null : curPage + 1);//下一页
		map.put("previousPage", curPage == 1 ? null : curPage - 1);  //上一页
		return map;
	}

	private static Class<?> getClass(String table){
		try {
			return Class.forName(entityPackage + "." + Utils.toHump(table));
		} catch (ClassNotFoundException e) {
			throw new JDBCException("未找到类："+ Utils.toHump(table));
		}
	}

	//根据对象设置where 条件
	private List<Object> setWhere(String table, Object obj, StringBuilder sql, String operators){
		if(operators.endsWith(",")){
			operators = operators+" ";
		}
		String[] poers = operators.split(",");
		List<Object> params = new ArrayList<>();
		Class<?> clazz = obj.getClass();
		List<Field> fields = Utils.getFields(clazz);
		for (int i = 0, j = 0; i < fields.size(); i++, j++) {
			// 剩余没指定运算符的字段直接跳过
			if (j == poers.length) {
				break;
			}
			String poer = poers[j].trim();
			if (poer.isEmpty())//是条件字段吗
				continue;
			if (poer.length() == 1 && poer.charAt(0) >= '1' && poer.charAt(0) <= '9') {
				i += Integer.parseInt(poer) - 1;
				continue;
			}
			fields.get(i).setAccessible(true);
			Object value;
			try {
				value = fields.get(i).get(obj);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new JDBCException("获取对象字段值异常");
			}
			if (!poer.startsWith("is") && !poer.endsWith("''")) //开启分页状态才去掉 null 和 ""
				if (value == null || value.equals(""))
					continue;

			String fieldName = getColumnNames(fields.get(i).getName());
			sql.append(params.size() == 0 ? " WHERE " : " AND ");//是第一个条件吗
			if (poer.startsWith("is") || poer.endsWith("''")) {
				sql.append(fieldName).append(" ").append(poer);
				continue;
			}
			switch (poer.endsWith(" in")?"in":poer) {
				case "in" 	 : sql.append(poer).append(" (?..)")							;break;
				case "like"	 : sql.append(fieldName).append(" like concat('%',?,'%')")		;break;
				case "like%" : sql.append(fieldName).append(" like concat(?,'%')")			;break;
				case "%like" : sql.append(fieldName).append(" like concat('%',?)")			;break;
				default		 : sql.append(fieldName).append(" ").append(poer).append(" ?")	;break;
			}
			params.add(value);
		}
		return params;
	}

	//设置对象自增id
	private void setObjAutoId(Object obj,String idName) {
		Class<?> clazz = obj.getClass();
		try {
			Field field = Utils.getField(clazz,idName);
			if(field == null){
				throw new JDBCException("未获得id字段");
			}
			field.setAccessible(true);
			Object id = selOne("1", "SELECT @@IDENTITY");//返回bigInteger
			field.set(obj, Integer.parseInt(id.toString()));
		}catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
			throw new JDBCException("设置id失败");
		}
	}

	// 给sql设置in条件
	private Object[] setIn(StringBuilder sql, Object[] params) {
		int index = sql.indexOf("(?..)");
		if (index == -1) {
			return params;
		}
		Object[] arr = new Object[0];
		Object[] params2 = new Object[0];
		int arrIndex = 0;
		for (int i = 0; i < params.length; i++) {
			if (params[i].getClass().isArray()) {
				arr = (Object[]) params[i];
				params2 = new Object[params.length + arr.length - 1];
				arrIndex = i;
				break;
			}
			if (params[i] instanceof List) {
				arr = ((List) params[i]).toArray();
				params2 = new Object[params.length + arr.length - 1];
				arrIndex = i;
				break;
			}
		}
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < params.length; i++) {
			if (i < arrIndex) {
				params2[i] = params[i];
			}
			if (i == arrIndex) {
				for (int j = 0; j < arr.length; j++) {
					params2[i + j] = arr[j];
					str.append("?,");
				}
				str.deleteCharAt(str.length() - 1);
			}
			if (i > arrIndex) {
				params2[i + arr.length - 1] = params[i];
			}
		}
		str.append(sql.substring(index+4));
		sql.replace(index+1,index+1+str.length(),str.toString());
		return params2;
	}

	//为sql拼装set和为params添加值
	private static void addSet(StringBuilder sql, Map<String, Object> map, Object[] params) {
		Set<String> keySet = map.keySet();
		int i = 0;
		for (String key : keySet) {
			sql.append(getColumnNames(key)).append("=?,");
			params[i++] = map.get(key);
		}
		sql.setCharAt(sql.length() - 1, ' ');

	}

	//获得字段名
	private static String getColumnNames(String name) {
		return autoToLine ? Utils.toLine(name) : name;
	}

	//获得列名
	private static String[] getColumnNames(String cNames, ResultSetMetaData meta)
			throws SQLException {
		String[] columnName;
		// 获取列数
		int count = meta.getColumnCount();
		//拿到全部列名
		if (cNames.equals("*")) {
			columnName = new String[count];
			for (int i = 0; i < count; i++)
				columnName[i] = meta.getColumnLabel(i + 1);
		} else {
			String[] ss = cNames.split(",");
			count = ss.length;
			columnName = new String[count];
			for (int i = 0; i < count; i++)
				columnName[i] = ss[i].trim();
		}
		return columnName;
	}

	// 获取总记录数 count
	private static int getTotalCount(SqlSession sqlSession, String statementName, Object values) {
		Map parameterMap = Utils.toMap(values);
		int count = 0;
		try {
			MappedStatement mst = sqlSession.getConfiguration().getMappedStatement(statementName);
			BoundSql boundSql = mst.getBoundSql(parameterMap);
			String sql = " select count(*) total_count from (" + boundSql.getSql() + ") count_sql ";
			PreparedStatement pstmt = sqlSession.getConnection().prepareStatement(sql);
			setParameters(pstmt, mst, boundSql, parameterMap);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				count = rs.getInt("total_count");
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			throw new JDBCException(e);
		}
		return count;
	}

	// 对SQL参数(?)设值,参考org.apache.ibatis.executor.parameter.DefaultParameterHandler
	private static void setParameters(PreparedStatement ps, MappedStatement mappedStatement, BoundSql boundSql,
                                      Object parameterObject)
			throws SQLException {
		ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
		List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
		if (parameterMappings == null) {
			return;
		}
		Configuration configuration = mappedStatement.getConfiguration();
		TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
		MetaObject metaObject = parameterObject == null ? null : configuration.newMetaObject(parameterObject);
		for (int i = 0; i < parameterMappings.size(); i++) {
			ParameterMapping parameterMapping = parameterMappings.get(i);
			if (parameterMapping.getMode() != ParameterMode.OUT) {
				Object value;
				String propertyName = parameterMapping.getProperty();
				PropertyTokenizer prop = new PropertyTokenizer(propertyName);
				if (parameterObject == null) {
					value = null;
				} else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
					value = parameterObject;
				} else if (boundSql.hasAdditionalParameter(propertyName)) {
					value = boundSql.getAdditionalParameter(propertyName);
				} else if (propertyName.startsWith(ForEachSqlNode.ITEM_PREFIX) && boundSql.hasAdditionalParameter(prop.getName())) {
					value = boundSql.getAdditionalParameter(prop.getName());
					if (value != null) {
						value = configuration.newMetaObject(value).getValue(propertyName.substring(prop.getName().length()));
					}
				} else {
					value = metaObject == null ? null : metaObject.getValue(propertyName);
				}
				TypeHandler typeHandler = parameterMapping.getTypeHandler();
				if (typeHandler == null) {
					throw new ExecutorException("There was no TypeHandler found for parameter " + propertyName
							+ " of statement " + mappedStatement.getId());
				}
				typeHandler.setParameter(ps, i + 1, value, parameterMapping.getJdbcType());
			}
		}
	}

	static class MyParam{
		public int curPage;
		public int pageSize;
		public Integer count;
		public String sql;
		public Object[] params;
		public PreparedStatement preparedStatement;
		public List list;

		MyParam(long count,Object[] params,int curPage, int pageSize,String sql){
			this.curPage=curPage;
			this.pageSize=pageSize;
			this.sql=sql;
			this.params=params;
			this.count= Math.toIntExact(count);
		}
		MyParam(String sql, Object[] params){
			this.sql=sql;
			this.params=params;
		}
		MyParam(){

		}
	}

	static class Utils {

		private static final String header = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"http://mybatis.org/dtd/mybatis-3-mapper.dtd\" >\n";
		private static final String title = "\n<!--\n\tTable: #{table}\n\tComments: #{comments}\n-->\n\n";
		private static final String mapperHead = "<mapper namespace=\"#{namespace}\">\n\n";
		private static final String selectHead = "\t<select id=\"select\" resultMap=\"myResultMap\" parameterType=\"#{parameterType}\">\n";
		private static final String insertHead = "\t<insert id=\"insert\" parameterType=\"#{parameterType}\">\n";
		private static final String updateByIdHead = "\t<update id=\"#{updateById}\" parameterType=\"#{parameterType}\">\n";
		private static final String deleteByIdHead = "\t<delete id=\"#{deleteById}\">\n";
		private static final String selectByIdHead = "\t<select id=\"#{selectById}\" resultType=\"#{parameterType}\">\n";
		private static final String resultMapHead = "\t<resultMap id=\"myResultMap\" type=\"#{parameterType}\">\n";
		private static final String autoIdHead = "\t\t<selectKey resultType=\"int\" keyProperty=\"#{autoId}\" order=\"AFTER\" >\n";

		private static String table;
		private static String primaryKey;
		private static String namespace;
		private static String parameterType;
		private static List<String> columns;
		private static List<String> fields;
		private static String columnsSql;
		private static String filePath;

		// 基于entity生成Table类
		public static void createTable(String entityPath,String mapperPack){
			List<String> namelist = Stream.of(Objects.requireNonNull(new File(entityPath).listFiles()))
					.filter(File::isFile)
					.map(File::getName)
					.map(name -> name.substring(0,name.indexOf(".")))
					.collect(Collectors.toList());
			StringBuilder tableFile = new StringBuilder();
			String entiypack = entityPath.substring(entityPath.indexOf("com")).replace("\\",".");
			tableFile.append("package ").append(entiypack).append(";\n\n")
				 	.append("public class Table {\n\n")
					.append("\tpublic static final String entityPackage = \"").append(entiypack).append("\";\n")
					.append("\tpublic static final String mapperPackage = \"").append(mapperPack).append("\";\n\n");
			namelist.forEach(name -> {
				tableFile.append("\tpublic static final String ").append(name).append(" = \"").append(name).append("\";\n");
			});
			tableFile.append("\n}");
			write(new File(entityPath,"Table.Java"),tableFile.toString());
		}
		/**
		 * 根据类生成Mapper文件
		 *
		 * @param clazz		表对应的class
		 * @param table		表名
		 * @param primaryKey 主键
		 * @param comments	说明
		 * @param namespace	命名空间
		 */
		public static void createMapper(Class<?> clazz,String table, String primaryKey, String comments, String namespace) {
			//初始化
			table = table;
			namespace = namespace;
			primaryKey = (primaryKey==null||primaryKey.isEmpty())?null:primaryKey;

			parameterType = clazz.getName();
			String fileName = clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1) + "Mapper.xml";
			Field[] fields2 = clazz.getDeclaredFields();
			fields = new ArrayList<>();
			columns = new ArrayList<>();
			for (Field field : fields2) {
				if (field.getType().equals(List.class)) {
					continue;
				}
				fields.add(field.getName());
				columns.add(autoToLine ? toLine(field.getName()) : field.getName());
			}
			columnsSql = Arrays.toString(columns.toArray()).replaceAll("[\\[\\]]", "");

			//拼装
			String file = header +
					title.replace("#{table}", table).replace("#{comments}", comments) +
					getMapper();
			write(new File(filePath, fileName), file);
		}

		private static String getMapper() {
			return mapperHead.replace("#{namespace}", namespace) +
					resultMap() +
					select() +
					insert() +
					update() +
					delete() +
					"</mapper>";
		}

		private static StringBuilder resultMap() {
			StringBuilder resultMap = new StringBuilder();
			resultMap.append(resultMapHead.replace("#{parameterType}", parameterType));
			for (int i = 0; i < columns.size(); i++) {
				resultMap.append("\t\t<").append(primaryKey!=null&&primaryKey.equals(fields.get(i))?"id":"result")
						.append(" property=\"").append(fields.get(i)).append("\" column=\"").append(columns.get(i)).append("\"/>\n");
			}
			resultMap.append("\t</resultMap>\n\n");
			return resultMap;
		}

		private static StringBuilder select() {
			StringBuilder select = new StringBuilder();
			if(primaryKey != null) {
				select.append(selectByIdHead.replace("#{selectById}", "selectBy" + toHump(primaryKey))
						.replace("#{parameterType}", parameterType))
						.append("\t\tselect ").append(columnsSql).append("\n\t\tfrom ").append(table).append(" where ")
						.append(toLine(primaryKey)).append(" = ").append(val(primaryKey))
						.append("\n\t</select>\n\n");
			}
			select.append(selectHead.replace("#{parameterType}", parameterType))
					.append("\t\tselect ").append(columnsSql).append("\n\t\tfrom ").append(table).append("\n")
					.append(getWhere()).append("\n")
					.append("\t</select>\n\n");
			return select;
		}

		private static StringBuilder insert() {
			StringBuilder insert = new StringBuilder();
			insert.append(insertHead.replace("#{parameterType}", parameterType));
			if(primaryKey!=null) {
				insert.append(autoIdHead.replace("#{autoId}",primaryKey))
						.append("\t\t\tSELECT LAST_INSERT_ID()\n")
						.append("\t\t</selectKey>\n");
			}
			insert.append("\t\tinsert into ").append(table).append("(").append(columnsSql).append(") \n\t\tvalues(")
					.append(getFieldsSql()).append(")\n")
					.append("\t</insert>\n\n");
			return insert;
		}

		private static StringBuilder update() {
			StringBuilder update = new StringBuilder();
			if(primaryKey != null)
				update.append(updateByIdHead.replace("#{updateById}", "updateBy" + toHump(primaryKey))
						.replace("#{parameterType}", parameterType))
						.append("\t\tupdate ").append(table).append("\n").append(getSet())
						.append("\t\twhere ").append(toLine(primaryKey)).append(" = ").append(val(primaryKey)).append("\n")
						.append("\t</update>\n\n");
			return update;
		}

		private static StringBuilder delete() {
			StringBuilder delete = new StringBuilder();
			if(primaryKey != null)
				delete.append(deleteByIdHead.replace("#{deleteById}", "deleteBy" + toHump(primaryKey)))
						.append("\t\tdelete from ").append(table).append(" where ")
						.append(toLine(primaryKey)).append(" = ").append(val(primaryKey)).append("\n")
						.append("\t</delete>\n\n");
			return delete;
		}

		private static StringBuilder getSet() {
			StringBuilder set = new StringBuilder();
			set.append("\t\t<set>\n");
			for (int i = 0; i < columns.size(); i++) {
				if(columns.get(i).equals(toLine("updateTime"))){
					set.append("\t\t\t").append(toLine("updateTime")).append("=now(),\n");
					continue;
				}
				set.append("\t\t\t<if test=\"").append(fields.get(i)).append(" != null\">\n")
						.append("\t\t\t\t").append(columns.get(i)).append(" = ").append(val(fields.get(i))).append(",\n")
						.append("\t\t\t</if>\n");
			}
			set.append("\t\t</set>\n");
			return set;
		}

		private static StringBuilder getWhere() {
			StringBuilder where = new StringBuilder();
			where.append("\t\t<where>\n");
			for (int i = 0; i < columns.size(); i++) {
				where.append("\t\t\t<if test=\"").append(fields.get(i)).append(" != null\">\n")
						.append("\t\t\t\t").append("and ").append(columns.get(i)).append(" = ").append(val(fields.get(i))).append("\n")
						.append("\t\t\t</if>\n");
			}
			where.append("\t\t</where>\n");
			return where;
		}

		private static StringBuilder getFieldsSql() {
			StringBuilder sb = new StringBuilder();
			for (String field : fields) {
				if (field.equals("createTime")) {
					sb.append("now(), ");
					continue;
				}
				if (field.equals("updateTime")) {
					sb.append("now(), ");
					continue;
				}
				sb.append(val(field)).append(", ");
			}
			sb.delete(sb.length()-2,sb.length());
			return sb;
		}

		private static String val(String str) {
			return "#{"+str+"}";
		}

		public static void write(File file, String  str) {
			if(file == null)
				throw new RuntimeException("文件为null");
			if(file.isDirectory())
				throw new RuntimeException("只支持文件");
			if(str == null)
				throw new RuntimeException("内容不能为null");
			try (
					OutputStream out = new FileOutputStream(file,false);
					OutputStreamWriter osw = new OutputStreamWriter(out, StandardCharsets.UTF_8);
					BufferedWriter bw = new BufferedWriter(osw)){
				bw.write(str);
				bw.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public static void setFilePath(String filePath) {
			Utils.filePath = filePath;
		}

		// 获取某个字段
		public static Field getField(Class clazz, String idName){
			if(clazz == Object.class){
				return null;
			}
			Field field;
			try {
				field = clazz.getDeclaredField(idName);
			} catch (NoSuchFieldException e) {
				return getField(clazz.getSuperclass(),idName);
			}
			return field;
		}

		// 是数字类型吗
		public static boolean isNumber(Object value){
			if(value==null)
				return false;
			return 	value instanceof Byte ||
					value instanceof Short ||
					value instanceof Integer ||
					value instanceof Float ||
					value instanceof Double ||
					value instanceof Long ||
					value instanceof BigInteger ||
					value instanceof BigDecimal;
		}
		//首字母大写
		private static String captureName(String str) {
			return str.substring(0, 1).toUpperCase() + str.substring(1);
		}

		//忽略大小写查找第一个
		public static int getLastIndex(String sql, String str) {
			int i = sql.lastIndexOf(str);
			return i == -1 ? sql.lastIndexOf(str.toLowerCase()) : i;
		}

		//忽略大小写查找最后一个
		public static int getStartIndex(String sql, String str) {
			int i = sql.lastIndexOf(str);
			return i == -1 ? sql.lastIndexOf(str.toLowerCase()) : i;
		}

		//忽略大小写查找最后一个
		public static int getStartIndex(StringBuilder sql, String str) {
			int i = sql.lastIndexOf(str);
			return i == -1 ? sql.lastIndexOf(str.toLowerCase()) : i;
		}

		//驼峰转下划线
		public static String toLine(String str) {
			return str.replaceAll("[A-Z]", "_$0").toLowerCase();
		}

		//下划线转驼峰
		public static String toHump(String str) {
			int _c = str.indexOf("_");
			if (_c < 1) return str;
			StringBuilder sb = new StringBuilder(str);
			while (_c != -1) {
				char ch = sb.charAt(_c + 1);
				if (ch >= 'A' && ch <= 'z') ch -= 32;
				sb.setCharAt(_c + 1, ch);
				sb.deleteCharAt(_c);
				_c = sb.indexOf("_", _c);
			}
			return sb.toString();
		}

		// 获取对象全部字段包括父
		public static List<Field> getFields(Class<?> clazz) {
			if (clazz == null) {
				return new ArrayList<>();
			}
			List<Field> fields = getFields(clazz.getSuperclass());
			Collections.addAll(fields, clazz.getDeclaredFields());
			return fields;
		}

		//对象转map
		public static Map<String, Object> toMap(Object obj) {
			return toMap(obj,null,false);
		}

		public static Map<String, Object> toMap(Object obj, Class clazz, boolean isInsert) {
			if (obj == null) {
				return new HashMap();
			}
			if (obj instanceof Map) {
				return (Map<String, Object>) obj;
			}
			Map<String, Object> map = new HashMap<>();
			try {
				for (Field field : getFields(clazz == null ? obj.getClass() : clazz)) {
					field.setAccessible(true);
					String fieldName = field.getName();
					Object value;
					value = field.get(obj);
					if (isInsert && fieldName.equals("createTime") || clazz != null && fieldName.equals("updateTime")) {
						value = new Date();
					}
					if (value != null)
						map.put(fieldName, value);
				}
			}catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new JDBCException("获取对象字段值异常");
			}
			return map;
		}

		//查找第num个str在data中的位置
		public static int getIndexOf(StringBuilder data, int num, String str) {
			int t = getStartIndex(data, str);
			for (int i = 1; i < num; i++) {
				int temp = data.indexOf(str, t + str.length());
				t = temp == -1 ? data.indexOf(str.toLowerCase(), t + str.length()) : temp;
			}
			return t;
		}

		//字符串str在data中出现了几次
		public static int countStr(String data, String str) {
			Pattern pattern = Pattern.compile("(?i)" + str);//忽略大小写
			Matcher findMatcher = pattern.matcher(data);
			int i = 0;
			while (findMatcher.find()) {
				i++;
			}
			return i;
		}

		public static String setParams(String str,Object[] params){
			return setParams(str,params,false,-1,0);
		}

		public static String setParams(String str,Object[] params,boolean isSql,int curPage,int pageSize){
			StringBuilder sb  = new StringBuilder(str);
			for (Object o : params) {
				int i = sb.indexOf("?");
				if(i!=-1){
					sb.replace(i,i+1,isSql?(Utils.isNumber(o)?o.toString():("'"+o.toString()+"'")):o.toString());
				}
			}
			if(curPage!=-1){
				int i = sb.indexOf("?");
				if(i!=-1){
					sb.replace(i,i+1,curPage+"");
				}
				i = sb.indexOf("?");
				if(i!=-1){
					sb.replace(i,i+1,pageSize+"");
				}
			}
			return sb.toString();
		}
	}

	//静态代码 加载sql映射文件
	static {
		try {
			sqlconfig = new Properties();
			//获得sql 配置文件
			InputStream resourceAsStream = JDBC13.class.getClassLoader().getResourceAsStream("sql-config.properties");
			if(resourceAsStream != null){
				sqlconfig.load(resourceAsStream);
			}
		} catch (Exception e) {
			logInfo("未读取到sql-config");
		}
	}

	//获得连接对象
	public Connection getCon() {
		if(con==null) {
			try {
				con=ds.getConnection();
			} catch (SQLException e) {
				throw new JDBCException(e);
			}
		}threadNum++;
		return con;
	}

	public static String getEntityPackage() {
		return entityPackage;
	}

	public static void setEntityPackage(String entityPackage) {
		JDBC13.entityPackage = entityPackage;
	}

	public static String getMapperPackage() {
		return mapperPackage;
	}

	public static void setMapperPackage(String mapperPackage) {
		JDBC13.mapperPackage = mapperPackage;
	}

	private void setCon(Connection con) {
		this.con = con;
	}

	public static void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory){
		JDBC13.sqlSessionFactory = sqlSessionFactory;
	}

	/** 设置数据源 */
	public static void setDs(DataSource ds) {
		JDBC13.ds = ds;
	}

	/** 设置字段字段转驼峰 */
	public static void setAutoToHump(Boolean b) {
		autoToHump = b;
	}

	/** 设置自动驼峰转下划线 */
	public static void setAutoToLine(Boolean b){
		autoToLine=b;
	}

	public static void logSql(PreparedStatement pstatement,String sql,Object[] params,int curPage,int pageSize){
		assert pstatement != null;
		String endSql = pstatement.toString();
		if(endSql.contains(": ")) {
			System.out.println("JDBC==> sql: "+endSql.split(": ")[1]);
		}else {
			System.out.println("JDBC==> sql: "+ Utils.setParams(sql,params,true,curPage,pageSize));
		}
	}

	/**
	 * 日志打印，可以使用"?"作为占位符
	 * @param obj
	 * @param params
	 */
	public static void logInfo(Object obj,Object...params){
		String str = "";
		if(obj.getClass().isArray()){
			str = Arrays.toString((Object[])obj);
		}
		if(obj instanceof  String){
			str = Utils.setParams(obj.toString(),params,false,-1,0);
		}
		System.out.println("JDBC==> info: "+str);
	}

	public interface SqlSessionCallback {

		Object doInSession(SqlSession session);

	}
	static class JDBCException extends RuntimeException{

		private static final long serialVersionUID = -6783150465221567178L;

		public JDBCException(String msg){
			super(msg);
		}
		public JDBCException(Exception e){
			super(e);
		}
	}

}





