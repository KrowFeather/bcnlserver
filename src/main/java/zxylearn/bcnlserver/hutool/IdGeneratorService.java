package zxylearn.bcnlserver.hutool;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class IdGeneratorService {

    private final Snowflake snowflake;

    @SuppressWarnings("deprecation")
    public IdGeneratorService(
            @Value("${server.worker-id}") long workerId,
            @Value("${server.datacenter-id}") long datacenterId) {
        if (workerId < 0 || workerId > 31) {
            throw new IllegalArgumentException("workerId belongs to [0, 31]");
        }
        if (datacenterId < 0 || datacenterId > 31) {
            throw new IllegalArgumentException("datacenterId belongs to [0, 31]");
        }

        this.snowflake = IdUtil.createSnowflake(workerId, datacenterId);
    }

    public Long getId() {
        return snowflake.nextId();
    }
}