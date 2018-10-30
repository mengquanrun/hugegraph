/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */

package com.baidu.hugegraph.backend.store;

public interface MetaHandler<Session extends BackendSession> {

    public Object handle(Session session, String meta, Object... args);
}
