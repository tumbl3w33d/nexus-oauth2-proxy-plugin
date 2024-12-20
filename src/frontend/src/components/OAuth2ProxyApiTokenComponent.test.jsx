import Axios from 'axios';
import React from 'react';
import OAuth2ProxyApiTokenComponent from './OAuth2ProxyApiTokenComponent';

import '@testing-library/jest-dom'
import { act, render, fireEvent } from '@testing-library/react';

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

    it('renders page without immediately re-generating the token', async () => {
        const { findByText } = render(<OAuth2ProxyApiTokenComponent />);

        const token = await findByText(/Your current API token is hidden/i);
        expect(token).toBeInTheDocument();

        const button = await findByText(/Regenerate Token/i);
        expect(button).toBeInTheDocument();
        
        expect(Axios.post).toHaveBeenCalledTimes(0);
    });

    it('calls post request once when regenerate button is clicked', async () => {
        await act(async () => {
            const { findByText } = render(<OAuth2ProxyApiTokenComponent />);
            const button = await findByText(/Regenerate Token/i);
            fireEvent.click(button);
        });

        expect(Axios.post).toHaveBeenCalledTimes(1);
        expect(Axios.post).toHaveBeenCalledWith('/service/rest/oauth2-proxy/user/reset-token');
    });

    it('renders response content as token', async () => {
        await act(async () => {
            const { findByText } = render(<OAuth2ProxyApiTokenComponent />);
            const button = await findByText(/Regenerate Token/i);
            fireEvent.click(button);
            
            const token = await findByText(/foobar/i);
            expect(token).toBeInTheDocument();
        });
    });
});