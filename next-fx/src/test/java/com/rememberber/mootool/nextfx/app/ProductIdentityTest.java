package com.rememberber.mootool.nextfx.app;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductIdentityTest {

    @Test
    void developmentIdentityNeverChangesProductId() {
        ProductIdentity identity = ProductIdentity.fromArgs(new String[] {"--profile=dev"});
        assertThat(identity.development()).isTrue();
        assertThat(ProductIdentity.PRODUCT_ID).isEqualTo("next-fx");
        assertThat(identity.qualifiedBundleId()).isEqualTo("com.rememberber.mootool.next.fx.dev");
        assertThat(identity.displayName()).contains("Development");
        assertThat(ProductIdentity.WINDOWS_UPGRADE_CODE.toString()).isEqualTo("500dc26c-8050-4b1e-b7c6-691e1229bc00");
    }
}
