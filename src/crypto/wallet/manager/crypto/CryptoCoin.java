package crypto.wallet.manager.crypto;

import com.google.gson.annotations.SerializedName;

public record CryptoCoin(@SerializedName("asset_id") String offeringCode, String name,
                         @SerializedName("price_usd") double priceUSD, @SerializedName("type_is_crypto") int isCrypto) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CryptoCoin that = (CryptoCoin) o;

        return offeringCode.equals(that.offeringCode);
    }

    @Override
    public int hashCode() {
        return offeringCode.hashCode();
    }
}
