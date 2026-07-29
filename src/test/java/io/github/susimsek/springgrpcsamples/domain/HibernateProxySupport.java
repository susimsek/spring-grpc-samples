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

    static final class ProxyTodo extends Todo implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyTodo(Class<?> persistentClass) {
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

    static final class ProxyUser extends User implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyUser(Class<?> persistentClass) {
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

    static final class ProxyAuthority extends Authority implements HibernateProxy {

        private final LazyInitializer lazyInitializer;

        ProxyAuthority(Class<?> persistentClass) {
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
