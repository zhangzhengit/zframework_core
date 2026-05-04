package com.vo.zframework.core;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.vo.zframework.cache.J;
import com.vo.zframework.configuration.TempDir;

/**
 * session存sqlite
 */
// FIXME 2025年12月26日 17:49:21 zhangzhen :  记得除了connection其他的用完了都close
public class ZSessionDB {

	// SQLite 连接URL格式：jdbc:sqlite:数据库文件路径（绝对/相对）
	// 内存数据库（仅测试用）：jdbc:sqlite::memory:
	private static final String SQLITE_URL_PREFIX = "jdbc:sqlite:";
	private static final String datadir = TempDir.getUserDir() + File.separator + "data";

	static final String dbFilePath = datadir + File.separator + "session.db";

	static Connection connection = null;
	static {
		
		mkdir();
		connection = getConnection(dbFilePath);
		CREATETableIsNotExists();
		deleteExpired(System.currentTimeMillis());
		vacuum();
	}

	private static void mkdir() {
		final File dir = new File(datadir);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}

	public static void vacuum() {
		final String sql = "vacuum";

		try {
			connection.setAutoCommit(false);
			final PreparedStatement ps = connection.prepareStatement(sql);
			final int affectedRows = ps.executeUpdate();
			connection.commit();
		} catch (final SQLException e) {
			try {
				connection.rollback();
			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static ZSession findByid(final String sessionId) {
		System.out.println(
				Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZSessionDB.findByid()");
		
		final String sql =
				"SELECT"
						+ "  session_id,"
						+ "  create_time,"
						+ "  last_access_time,"
						+ "  interval_seconds,"
						+ "  content"
						+ " from "
						+ "  session"
						+ " where session_id = ?;";
		
		try {
			final PreparedStatement ps = connection.prepareStatement(sql);
			ps.setString(1, sessionId);
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				final ZSession session = new ZSession();
				session.setId(rs.getString(1));
				session.setCreateTime(rs.getTimestamp(2));
				session.setLastAccessedTime(rs.getTimestamp(3));
				session.setIntervalSeconds(rs.getLong(4));
				session.setData(J.parseObject(rs.getString(5), Map.class));
				return session;
			}
			
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		
		// 返回空对象，不返回null
		return new ZSession();
	}
	
	
	public static List<ZSession> loadValid(final long currentTimeMillis) {
		System.out.println(
				Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZSessionDB.loadValid()");
		final String sql =
				"SELECT"
				+ "  session_id,"
				+ "  create_time,"
				+ "  last_access_time,"
				+ "  interval_seconds,"
				+ "  content"
				+ " from "
				+ "  session"
				+ " where 1=1"
				+ "  and last_access_time > "+currentTimeMillis+"- interval_seconds * 1000"
				+ " order by last_access_time"
				+ " limit " + ZSessionMap.SessionMaxActiveInMemory + ";";

		try {
			final PreparedStatement ps = connection.prepareStatement(sql);
			final ResultSet rs = ps.executeQuery();
			final List<ZSession> l = new ArrayList<>();
			while (rs.next()) {
				final ZSession session = new ZSession();
				session.setId(rs.getString(1));
				session.setCreateTime(rs.getTimestamp(2));
				session.setLastAccessedTime(rs.getTimestamp(3));
				session.setIntervalSeconds(rs.getLong(4));
				// FIXME 2025年12月26日 13:56:32 zhangzhen : 序列华为map
				final Map<String, Object> object = J.parseObject(rs.getString(5), Map.class);
				session.setData(object);
				l.add(session);
			}

			return l;
		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static int deleteExpired(final long currentTimeMillis) {
		System.out.println(
				Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZSessionDB.loadValid()");
		final String sql =
						 " delete "
						+ " from "
						+ "  session"
						+ " where 1=1"
						// <= 过期的
						+ "  and last_access_time <= "+currentTimeMillis+"- interval_seconds * 1000";
		
		try {
			final PreparedStatement ps = connection.prepareStatement(sql);
			final int executeUpdate = ps.executeUpdate();
			System.out.println("deleteExpired.executeUpdate = " + executeUpdate);
			
			return executeUpdate;
		} catch (final SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	
	
	public static void delete(final String sessionId, final long createTime,
			final long lastAccessTime, final long intervalSeconds, final String content)  {
		
		
		final String insertSql = "INSERT INTO session (\r\n"
				+ "       session_id, create_time, last_access_time, interval_seconds,content\r\n"
				+ "   ) VALUES (?, ?, ?, ?, ?)";
		
		try {
			connection.setAutoCommit(false);
			final PreparedStatement ps = connection.prepareStatement(insertSql);
			ps.setString(1, sessionId);
			ps.setLong(2, createTime);
			ps.setLong(3, lastAccessTime);
			ps.setLong(4, intervalSeconds);
			ps.setString(5, content);
			
			final int affectedRows = ps.executeUpdate();
			connection.commit();
			System.out.println("插入成功，受影响行数：" + affectedRows);
		} catch (final SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public static void insertOrRepaceInto(final String sessionId, final long createTime,
			final long lastAccessTime, final long intervalSeconds, final String content)  {
		System.out.println(Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t"
				+ "ZSessionDB.insertOrRepaceInto()");
		
		final String insertSql = "INSERT OR REPLACE INTO session (\r\n"
				+ "       session_id, create_time, last_access_time, interval_seconds,content\r\n"
				+ "   ) VALUES (?, ?, ?, ?, ?)";
		
		try {
			connection.setAutoCommit(false);
			final PreparedStatement ps = connection.prepareStatement(insertSql);
			ps.setString(1, sessionId);
			ps.setLong(2, createTime);
			ps.setLong(3, lastAccessTime);
			ps.setLong(4, intervalSeconds);
			ps.setString(5, content);
			
			final int affectedRows = ps.executeUpdate();
			connection.commit();
			System.out.println("插入Or替换成功，受影响行数：" + affectedRows);
		} catch (final SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		}
	}
	public static void insert(final String sessionId, final long createTime,
			final long lastAccessTime, final long intervalSeconds, final String content)  {
		
		System.out
				.println(Thread.currentThread().getName() + "\t" + LocalDateTime.now() + "\t" + "ZSessionDB.insert()");
		
		final String insertSql = "INSERT INTO session (\r\n"
				+ "       session_id, create_time, last_access_time, interval_seconds,content\r\n"
				+ "   ) VALUES (?, ?, ?, ?, ?)";

		try {
			connection.setAutoCommit(false);
			final PreparedStatement ps = connection.prepareStatement(insertSql);
			ps.setString(1, sessionId);
			ps.setLong(2, createTime);
			ps.setLong(3, lastAccessTime);
			ps.setLong(4, intervalSeconds);
			ps.setString(5, content);

			final int affectedRows = ps.executeUpdate();
			connection.commit();
			System.out.println("插入成功，受影响行数：" + affectedRows);
		} catch (final SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (final SQLException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 创建 SQLite 数据库连接（推荐使用，带最佳配置）
	 * 
	 * @param dbFilePath 数据库文件路径（如：./data/myserver.db 或 C:/db/myserver.db）
	 * @return 可用的 Connection 对象
	 * @throws SQLException 连接创建失败时抛出
	 */
	private static Connection getConnection(final String dbFilePath) {
		final String jdbcUrl = SQLITE_URL_PREFIX + dbFilePath;
		Connection conn = null;
		try {
			conn = DriverManager.getConnection(jdbcUrl);
		} catch (final SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return conn;
	}

	private static void CREATETableIsNotExists() {
		final String createTableSql =
						   " CREATE TABLE IF NOT EXISTS session ("
						 + " session_id TEXT PRIMARY KEY,"
						 + " create_time timestamp NOT NULL,"
						 + " last_access_time timestamp NOT NULL,"
						 + " interval_seconds int NOT NULL,"
						 + " content TEXT);";
		
		try {
			final PreparedStatement pstmt = connection.prepareStatement(createTableSql);
			pstmt.executeUpdate();
		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

}