import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { describe, it, expect } from 'vitest';
import App from '../src/App.jsx';

describe('App', () => {
  it('renders the dashboard heading', () => {
    render(
      <BrowserRouter>
        <App />
      </BrowserRouter>,
    );
    expect(screen.getByText(/Smart Travel\s*&\s*Expense Hub/i)).toBeInTheDocument();
  });
});
