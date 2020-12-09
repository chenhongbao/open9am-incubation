/*
 * Copyright (C) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.open9am.engine;

import com.open9am.service.Account;
import com.open9am.service.CancelRequest;
import com.open9am.service.CancelResponse;
import com.open9am.service.Contract;
import com.open9am.service.INamedService;
import com.open9am.service.OrderRequest;
import com.open9am.service.OrderResponse;
import com.open9am.service.TraderRuntimeException;
import java.util.Collection;

/**
 * Handler for engine responses.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public interface ITraderEngineHandler extends INamedService {

    void onOrderReponse(OrderResponse response);

    void onCancelResponse(CancelResponse response);

    void onException(TraderRuntimeException exception);

    void onException(OrderRequest request, TraderRuntimeException exception, int requestId);

    void onException(CancelRequest request, TraderRuntimeException exception, int requestId);

    void onStatusChange(EngineStatus status);

    void onTraderServiceStatusChange(int status);

    void onErasingContracts(Collection<Contract> contracts);

    void onErasingAccount(Account account);
}
