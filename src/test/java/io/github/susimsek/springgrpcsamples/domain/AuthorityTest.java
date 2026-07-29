package io.github.susimsek.springgrpcsamples.domain;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.susimsek.springgrpcsamples.domain.HibernateProxySupport.ProxyAuthority;
import org.junit.jupiter.api.Test;

class AuthorityTest {

    @Test
    void accessorsAndEqualityWork() {
        Authority authority = new Authority();

        authority.setId(1L);
        authority.setName("ROLE_ADMIN");

        Authority same = new Authority(1L, "ROLE_USER");
        Authority different = new Authority(2L, "ROLE_ADMIN");
        Authority withoutId = new Authority(null, "ROLE_ADMIN");
        Authority otherWithoutId = new Authority(null, "ROLE_USER");

        assertThat(authority.getId()).isEqualTo(1L);
        assertThat(authority.getName()).isEqualTo("ROLE_ADMIN");
        assertThat(authority)
                .isEqualTo(authority)
                .isEqualTo(same)
                .isNotEqualTo(different)
                .isNotEqualTo(withoutId)
                .isNotEqualTo(otherWithoutId)
                .isNotEqualTo(null)
                .isNotEqualTo("authority");
        assertThat(authority.hashCode()).isEqualTo(Authority.class.hashCode());
        assertThat(withoutId).isNotEqualTo(authority);
    }

    @Test
    void equalitySupportsHibernateProxy() {
        Authority authority = new Authority(1L, "ROLE_ADMIN");
        ProxyAuthority proxy = new ProxyAuthority(Authority.class);
        proxy.setId(1L);

        assertThat(authority).isEqualTo(proxy);
        assertThat(proxy).isEqualTo(authority);
        assertThat(proxy.hashCode()).isEqualTo(Authority.class.hashCode());
    }
}
