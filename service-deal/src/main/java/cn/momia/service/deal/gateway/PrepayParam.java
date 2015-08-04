package cn.momia.service.deal.gateway;

public abstract class PrepayParam extends MapWrapper {
    private int tradeSourceType;

    public int getTradeSourceType() {
        return tradeSourceType;
    }

    public void setTradeSourceType(int tradeSourceType) {
        this.tradeSourceType = tradeSourceType;
    }

    public abstract long getOrderId();
}
