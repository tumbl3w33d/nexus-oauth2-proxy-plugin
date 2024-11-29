import Axios from 'axios';
import React, { act } from 'react';
import OAuth2ProxyApiTokenComponent from './OAuth2ProxyApiTokenComponent';

import '@testing-library/jest-dom'
import { render } from '@testing-library/react';

jest.mock('axios');

// mock private @sonatype/nexus-ui-plugin
const createMockComponent = () => jest.fn(({ children, ...props }) => <div {...props}>{children}</div>);
jest.mock('@sonatype/nexus-ui-plugin', () => ({
    Page: createMockComponent(),
    PageHeader: createMockComponent(),
    PageTitle: createMockComponent(),
    ContentBody: createMockComponent(),
    Section: createMockComponent(),
    SectionFooter: createMockComponent(),
}));

describe('OAuth2ProxyApiTokenComponent', () => {

    beforeEach(() => {
        Axios.post.mockImplementationOnce(() => Promise.resolve({ data: 'foobar' }));
    });

    it('calls post request once when rendered', async () => {
        await act(async () => {
            render(<OAuth2ProxyApiTokenComponent />);
        });

        expect(Axios.post).toHaveBeenCalledTimes(1);
        expect(Axios.post).toHaveBeenCalledWith('/service/rest/oauth2-proxy/user/reset-token');
    });

    it('renders response content as token', async () => {
        const { findByText } = render(<OAuth2ProxyApiTokenComponent />);

        const token = await findByText(/foobar/i);
        expect(token).toBeInTheDocument();
    });
});