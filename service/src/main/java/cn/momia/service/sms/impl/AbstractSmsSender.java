package cn.momia.service.sms.impl;

import cn.momia.common.config.Configuration;
import cn.momia.service.common.DbAccessService;
import cn.momia.service.sms.SmsSender;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.regex.Pattern;

public abstract class AbstractSmsSender extends DbAccessService implements SmsSender {
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[0-9]{10}$");

    protected Configuration conf;

    public void setConf(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public void send(String mobile)  {
        if (!MOBILE_PATTERN.matcher(mobile).find()) throw new RuntimeException("invalid mobile: " + mobile);

        String code = getGeneratedCode(mobile);
        if (StringUtils.isBlank(code))
        {
            boolean outOfDate = (code != null); // null 表示没有生成过，空表示生成过，但已过期
            code = generateCode(mobile);
            updateCode(mobile, code, outOfDate);
        }

        Date lastSendTime = getLastSendTime(mobile);
        if (lastSendTime != null && new Date().getTime() - lastSendTime.getTime() < 60 * 1000) return;

        sendAsync(mobile, code);
    }

    private void sendAsync(final String mobile, final String code) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (doSend(mobile, code))
                {
                    updateSendTime(mobile);
                }
            }
        }).start();
    }

    private String getGeneratedCode(String mobile)
    {
        String sql = "SELECT code, status, generateTime FROM t_verify WHERE mobile=?";

        return jdbcTemplate.query(sql, new Object[] { mobile }, new ResultSetExtractor<String>()
        {
            @Override
            public String extractData(ResultSet rs) throws SQLException, DataAccessException
            {
                if (!rs.next()) return null;

                String code = rs.getString("code");
                int status = rs.getInt("status");
                Date generateTime = rs.getTimestamp("generateTime");

                if (status == 0 || generateTime == null || new Date().getTime() - generateTime.getTime() > 30 * 60 * 1000) return "";
                return code;
            }
        });
    }

    private String generateCode(String mobile)
    {
        int number = (int) (Math.random() * 1000000);

        return String.format("%06d", number);
    }

    private void updateCode(String mobile, String code, boolean outOfDate)
    {
        if (outOfDate)
        {
            String sql = "UPDATE t_verify SET mobile=?, code=?, generateTime=NOW(), sendTime=NULL, status=1 WHERE mobile=?";
            jdbcTemplate.update(sql, new Object[] { mobile, code, mobile });
        }
        else
        {
            String sql = "INSERT INTO t_verify(mobile, code, generateTime) VALUES (?, ?, NOW())";
            jdbcTemplate.update(sql, new Object[] { mobile, code });
        }
    }

    private Date getLastSendTime(String mobile)
    {
        String sql = "SELECT sendTime FROM t_verify WHERE mobile=?";

        return jdbcTemplate.query(sql, new Object[] { mobile }, new ResultSetExtractor<Date>()
        {
            @Override
            public Date extractData(ResultSet rs) throws SQLException, DataAccessException
            {
                if (rs.next()) return rs.getTimestamp("sendTime");
                return null;
            }
        });
    }

    protected abstract boolean doSend(String mobile, String code);

    private boolean updateSendTime(String mobile)
    {
        String sql = "UPDATE t_verify SET sendTime=NOW() WHERE mobile=?";

        return jdbcTemplate.update(sql, new Object[] { mobile }) == 1;
    }
}
