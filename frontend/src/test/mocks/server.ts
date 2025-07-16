import { setupServer } from 'msw/node';
import { http, HttpResponse } from 'msw';

const API_BASE_URL = 'http://localhost:8080/api';

export const handlers = [
  // Company search
  http.get(`${API_BASE_URL}/v1/companies/search`, ({ request }) => {
    const url = new URL(request.url);
    const query = url.searchParams.get('query');
    
    if (query === 'Test Company') {
      return HttpResponse.json([
        {
          id: 1,
          companyNumber: '12345678',
          companyName: 'Test Company Ltd',
          companyType: 'LTD',
          incorporationDate: '2020-01-01',
          nzbn: '9429000000000',
          status: 'ACTIVE',
        },
        {
          id: 2,
          companyNumber: '87654321',
          companyName: 'Test Company 2 Ltd',
          companyType: 'LTD',
          incorporationDate: '2021-01-01',
          nzbn: '9429000000001',
          status: 'ACTIVE',
        },
      ]);
    }
    
    if (query === 'Empty') {
      return HttpResponse.json([]);
    }
    
    if (query === 'Error') {
      return HttpResponse.json(
        { message: 'Search error' },
        { status: 500 }
      );
    }
    
    return HttpResponse.json([]);
  }),

  // Get company by ID
  http.get(`${API_BASE_URL}/v1/companies/:id`, ({ params }) => {
    const { id } = params;
    
    if (id === '1') {
      return HttpResponse.json({
        id: 1,
        companyNumber: '12345678',
        companyName: 'Test Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2020-01-01',
        nzbn: '9429000000000',
        status: 'ACTIVE',
      });
    }
    
    if (id === '404') {
      return HttpResponse.json(
        { message: 'Company not found' },
        { status: 404 }
      );
    }
    
    return HttpResponse.json(
      { message: 'Company not found' },
      { status: 404 }
    );
  }),

  // Get company by number
  http.get(`${API_BASE_URL}/v1/companies/number/:number`, ({ params }) => {
    const { number } = params;
    
    if (number === '12345678') {
      return HttpResponse.json({
        id: 1,
        companyNumber: '12345678',
        companyName: 'Test Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2020-01-01',
        nzbn: '9429000000000',
        status: 'ACTIVE',
      });
    }
    
    return HttpResponse.json(
      { message: 'Company not found' },
      { status: 404 }
    );
  }),

  // Get active companies
  http.get(`${API_BASE_URL}/v1/companies`, () => {
    return HttpResponse.json([
      {
        id: 1,
        companyNumber: '12345678',
        companyName: 'Test Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2020-01-01',
        nzbn: '9429000000000',
        status: 'ACTIVE',
      },
      {
        id: 2,
        companyNumber: '87654321',
        companyName: 'Another Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2021-01-01',
        nzbn: '9429000000001',
        status: 'ACTIVE',
      },
    ]);
  }),

  // Create company
  http.post(`${API_BASE_URL}/v1/companies`, async ({ request }) => {
    const company = await request.json() as Record<string, any>;
    
    return HttpResponse.json({
      id: 3,
      ...company,
    }, { status: 201 });
  }),

  // Update company
  http.put(`${API_BASE_URL}/v1/companies/:id`, async ({ params, request }) => {
    const { id } = params;
    const updates = await request.json() as Record<string, any>;
    
    if (id === '1') {
      return HttpResponse.json({
        id: 1,
        companyNumber: '12345678',
        companyName: 'Test Company Ltd',
        companyType: 'LTD',
        incorporationDate: '2020-01-01',
        nzbn: '9429000000000',
        status: 'ACTIVE',
        ...updates,
      });
    }
    
    return HttpResponse.json(
      { message: 'Company not found' },
      { status: 404 }
    );
  }),

  // Delete company
  http.delete(`${API_BASE_URL}/v1/companies/:id`, ({ params }) => {
    const { id } = params;
    
    if (id === '1') {
      return HttpResponse.json(null, { status: 204 });
    }
    
    return HttpResponse.json(
      { message: 'Company not found' },
      { status: 404 }
    );
  }),

  // Check name availability
  http.get(`${API_BASE_URL}/v1/companies/check-name`, ({ request }) => {
    const url = new URL(request.url);
    const name = url.searchParams.get('name');
    
    return HttpResponse.json({
      available: name !== 'Taken Company Name',
    });
  }),

  // Check number availability
  http.get(`${API_BASE_URL}/v1/companies/check-number`, ({ request }) => {
    const url = new URL(request.url);
    const number = url.searchParams.get('number');
    
    return HttpResponse.json({
      available: number !== '12345678',
    });
  }),

  // Auth endpoints (mock)
  http.get('https://test.auth0.com/.well-known/jwks.json', () => {
    return HttpResponse.json({
      keys: [
        {
          kty: 'RSA',
          kid: 'test-key-id',
          use: 'sig',
          n: 'test-modulus',
          e: 'AQAB',
        },
      ],
    });
  }),

  // Unhandled requests
  http.all('*', ({ request }) => {
    console.warn(`Unhandled ${request.method} request to ${request.url}`);
    return HttpResponse.json(
      { message: 'Not found' },
      { status: 404 }
    );
  }),
];

export const server = setupServer(...handlers);