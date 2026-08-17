package com.heima.config;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

/**
 * AgentScope 2 默认 Redis 状态存储：key 为
 * {@code agentscope:session:{userId}/{sessionId}:agent_state}。
 */
@Configuration
public class AgentScopeConfig {

    @Bean(destroyMethod = "close")
    public JedisPooled jedisPooled(RedisProperties props) {
        DefaultJedisClientConfig.Builder cfg = DefaultJedisClientConfig.builder()
                .database(props.getDatabase());
        if (StringUtils.hasText(props.getPassword())) {
            cfg.password(props.getPassword());
        }
        return new JedisPooled(new HostAndPort(props.getHost(), props.getPort()), cfg.build());
    }

    @Bean
    public AgentStateStore agentStateStore(JedisPooled jedisPooled) {
        return RedisAgentStateStore.builder()
                .jedisClient(jedisPooled)
                .build();
    }
}
