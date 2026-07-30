package io.github.susimsek.springgrpcsamples.domain;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;

final class HibernateProxySupport {

    private HibernateProxySupport() {}

    static LazyInitializer lazyInitializer(Class<?> persistentClass) {
        LazyInitializer lazyInitializer = mock(LazyInitializer.class);
        doReturn(persistentClass).when(lazyInitializer).getPersistentClass();
        return lazyInitializer;
    }

    static final class ProxyTodoEntity extends TodoEntity implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyTodoEntity(Class<?> persistentClass) {
            this.lazyInitializer = lazyInitializer(persistentClass);
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return lazyInitializer;
        }
    }

    static final class ProxyUserEntity extends UserEntity implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyUserEntity(Class<?> persistentClass) {
            this.lazyInitializer = lazyInitializer(persistentClass);
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return lazyInitializer;
        }
    }

    static final class ProxyAuthorityEntity extends AuthorityEntity implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyAuthorityEntity(Class<?> persistentClass) {
            this.lazyInitializer = lazyInitializer(persistentClass);
        }

        @Override
        public Object writeReplace() {
            return this;
        }

        @Override
        public LazyInitializer getHibernateLazyInitializer() {
            return lazyInitializer;
        }
    }
}
