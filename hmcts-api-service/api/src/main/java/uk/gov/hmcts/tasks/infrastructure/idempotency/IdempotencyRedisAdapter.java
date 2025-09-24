package uk.gov.hmcts.tasks.infrastructure.idempotency;

import redis.clients.jedis.JedisPooled;
import uk.gov.hmcts.tasks.application.ports.IdempotencyStorePort;

public class IdempotencyRedisAdapter implements IdempotencyStorePort {
  private final JedisPooled jedis;

  public IdempotencyRedisAdapter(String redisUrl) {
    this.jedis = new JedisPooled(redisUrl);
  }

  @Override
  public boolean tryStore(String key, String fingerprint) {
    Long set = jedis.setnx(key, fingerprint);
    return set != null && set == 1L;
  }
}
