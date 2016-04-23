/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.git.client.checkout;

import org.eclipse.che.api.git.shared.CheckoutRequest;
import org.eclipse.che.api.promises.client.Operation;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.ext.git.client.BaseTest;
import org.eclipse.che.ide.resource.Path;
import org.junit.Test;
import org.mockito.Mock;

import static org.eclipse.che.ide.ext.git.client.checkout.CheckoutReferencePresenter.CHECKOUT_COMMAND_NAME;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Testing {@link CheckoutReferencePresenter} functionality.
 *
 * @author Roman Nikitenko
 * @author Vlad Zhukovskyi
 */
public class CheckoutReferenceTest extends BaseTest {
    private static final String CORRECT_REFERENCE   = "someTag";
    private static final String INCORRECT_REFERENCE = "";

    @Mock
    private CheckoutReferenceView view;
    @Mock
    private CheckoutRequest       checkoutRequest;

    private CheckoutReferencePresenter presenter;

    @Override
    public void disarm() {
        super.disarm();

        presenter = new CheckoutReferencePresenter(view,
                                                   service,
                                                   appContext,
                                                   constant,
                                                   notificationManager,
                                                   gitOutputConsoleFactory,
                                                   consolesPanelPresenter,
                                                   workspace,
                                                   dtoFactory);
    }

    @Test
    public void testOnReferenceValueChangedWhenValueIsIncorrect() throws Exception {
        presenter.referenceValueChanged(INCORRECT_REFERENCE);

        view.setCheckoutButEnableState(eq(false));
    }

    @Test
    public void testOnReferenceValueChangedWhenValueIsCorrect() throws Exception {
        presenter.referenceValueChanged(CORRECT_REFERENCE);

        view.setCheckoutButEnableState(eq(true));
    }

    @Test
    public void testShowDialog() throws Exception {
        presenter.showDialog(project);

        verify(view).setCheckoutButEnableState(eq(false));
        verify(view).showDialog();
    }

    @Test
    public void testOnCancelClicked() throws Exception {
        presenter.onCancelClicked();

        verify(view).close();
    }


    @Test
    public void onEnterClickedWhenValueIsIncorrect() throws Exception {
        reset(service);
        when(view.getReference()).thenReturn(INCORRECT_REFERENCE);

        presenter.onEnterClicked();

        verify(view, never()).close();
        verify(service, never()).checkout(anyString(), any(Path.class), any(CheckoutRequest.class));
    }

    @Test
    public void onEnterClickedWhenValueIsCorrect() throws Exception {
        when(dtoFactory.createDto(CheckoutRequest.class)).thenReturn(checkoutRequest);
        when(checkoutRequest.withName(anyString())).thenReturn(checkoutRequest);
        when(checkoutRequest.withCreateNew(anyBoolean())).thenReturn(checkoutRequest);
        reset(service);
        when(service.checkout(anyString(), any(Path.class), any(CheckoutRequest.class))).thenReturn(voidPromise);
        when(voidPromise.then(any(Operation.class))).thenReturn(voidPromise);
        when(voidPromise.catchError(any(Operation.class))).thenReturn(voidPromise);
        when(view.getReference()).thenReturn(CORRECT_REFERENCE);

        presenter.showDialog(project);
        presenter.onEnterClicked();

        verify(voidPromise).then(voidPromiseCaptor.capture());
        voidPromiseCaptor.getValue().apply(null);

        verify(synchronizePromise).then(synchronizeCaptor.capture());
        synchronizeCaptor.getValue().apply(new Resource[0]);

        verify(view).close();
        verify(service).checkout(anyString(), any(Path.class), any(CheckoutRequest.class));
        verify(checkoutRequest).withName(CORRECT_REFERENCE);
        verifyNoMoreInteractions(checkoutRequest);
    }

    @Test
    public void testOnCheckoutClickedWhenCheckoutIsFailed() throws Exception {
        reset(service);
        when(dtoFactory.createDto(CheckoutRequest.class)).thenReturn(checkoutRequest);
        when(checkoutRequest.withName(anyString())).thenReturn(checkoutRequest);
        when(checkoutRequest.withCreateNew(anyBoolean())).thenReturn(checkoutRequest);
        when(service.checkout(anyString(), any(Path.class), any(CheckoutRequest.class))).thenReturn(voidPromise);
        when(voidPromise.then(any(Operation.class))).thenReturn(voidPromise);
        when(voidPromise.catchError(any(Operation.class))).thenReturn(voidPromise);

        when(view.getReference()).thenReturn(CORRECT_REFERENCE);

        presenter.showDialog(project);
        presenter.onEnterClicked();

        verify(voidPromise).catchError(promiseErrorCaptor.capture());
        promiseErrorCaptor.getValue().apply(promiseError);

        verify(checkoutRequest).withName(CORRECT_REFERENCE);
        verifyNoMoreInteractions(checkoutRequest);
        verify(view).close();
        verify(gitOutputConsoleFactory).create(CHECKOUT_COMMAND_NAME);
        verify(console).printError(anyString());
        verify(consolesPanelPresenter).addCommandOutput(anyString(), eq(console));
        verify(notificationManager).notify(anyString(), anyObject(), anyBoolean());
    }
}
