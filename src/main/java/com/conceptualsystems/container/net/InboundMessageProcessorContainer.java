package com.conceptualsystems.container.net;

import android.content.Context;

import com.conceptualsystems.android.net.InboundMessageProcessorAndroid;

public class InboundMessageProcessorContainer extends InboundMessageProcessorAndroid {
    protected Context mContext;

    public InboundMessageProcessorContainer(Context context) {
        super(context);
        mContext = context;
        mRequestProcessor = new RequestProcessorContainer();
        mResponseProcessor = new ResponseProcessorContainer();
    }

    public class RequestProcessorContainer extends RequestProcessorAndroid {

    }

    public class ResponseProcessorContainer extends ResponseProcessorAndroid {

    }
}
