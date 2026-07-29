package io.github.susimsek.springgrpcsamples.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.domain.HibernateProxySupport.ProxyUser;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void accessorsAndEqualityWork() {
        Authority authority = new Authority(1L, "ROLE_USER");
        HashSet<Authority> authorities = new HashSet<>();
        authorities.add(authority);
        User user = new User();
        Instant createdAt = Instant.EPOCH;
        Instant updatedAt = Instant.EPOCH.plusSeconds(1);

        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("secret");
        user.setEnabled(true);
        user.setAuthorities(authorities);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);

        User same = new User(1L, "other", "other", false, new HashSet<>());
        User different = new User(2L, "admin", "secret", true, authorities);
        User withoutId = new User(null, "admin", "secret", true, authorities);
        User otherWithoutId = new User(null, "other", "other", false, new HashSet<>());

        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getPassword()).isEqualTo("secret");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getAuthorities()).containsExactly(authority);
        assertThat(user.getCreatedAt()).isEqualTo(createdAt);
        assertThat(user.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(new User().getAuthorities()).isEmpty();
        assertThat(user)
                .isEqualTo(user)
                .isEqualTo(same)
                .isNotEqualTo(different)
                .isNotEqualTo(withoutId)
                .isNotEqualTo(otherWithoutId)
                .isNotEqualTo(null)
                .isNotEqualTo("user");
        assertThat(user.hashCode()).isEqualTo(User.class.hashCode());
        assertThat(withoutId).isNotEqualTo(user);
    }

    @Test
    void equalitySupportsHibernateProxy() {
        User user = new User(1L, "admin", "secret", true, new HashSet<>());
        ProxyUser proxy = new ProxyUser(User.class);
        proxy.setId(1L);

        assertThat(user).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(user);
        assertThat(proxy.hashCode()).isEqualTo(User.class.hashCode());
    }
}
