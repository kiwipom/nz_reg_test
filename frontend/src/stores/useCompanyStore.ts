import { create } from 'zustand';
import { devtools } from 'zustand/middleware';

interface Company {
  id: number;
  companyNumber: string;
  companyName: string;
  companyType: 'LTD' | 'OVERSEAS' | 'UNLIMITED';
  incorporationDate: string;
  nzbn?: string;
  status: string;
}

interface CompanyState {
  companies: Company[];
  selectedCompany: Company | null;
  loading: boolean;
  error: string | null;
  searchQuery: string;
  searchResults: Company[];
  
  // Actions
  setCompanies: (companies: Company[]) => void;
  setSelectedCompany: (company: Company | null) => void;
  setLoading: (loading: boolean) => void;
  setError: (error: string | null) => void;
  setSearchQuery: (query: string) => void;
  setSearchResults: (results: Company[]) => void;
  clearSearch: () => void;
  addCompany: (company: Company) => void;
  updateCompany: (id: number, updates: Partial<Company>) => void;
  removeCompany: (id: number) => void;
}

export const useCompanyStore = create<CompanyState>()(
  devtools(
    (set) => ({
      companies: [],
      selectedCompany: null,
      loading: false,
      error: null,
      searchQuery: '',
      searchResults: [],

      setCompanies: (companies) => set({ companies }),
      
      setSelectedCompany: (company) => set({ selectedCompany: company }),
      
      setLoading: (loading) => set({ loading }),
      
      setError: (error) => set({ error }),
      
      setSearchQuery: (query) => set({ searchQuery: query }),
      
      setSearchResults: (results) => set({ searchResults: results }),
      
      clearSearch: () => set({ searchQuery: '', searchResults: [] }),
      
      addCompany: (company) => set(state => ({
        companies: [...state.companies, company]
      })),
      
      updateCompany: (id, updates) => set(state => ({
        companies: state.companies.map(company =>
          company.id === id ? { ...company, ...updates } : company
        ),
        selectedCompany: state.selectedCompany?.id === id
          ? { ...state.selectedCompany, ...updates }
          : state.selectedCompany
      })),
      
      removeCompany: (id) => set(state => ({
        companies: state.companies.filter(company => company.id !== id),
        selectedCompany: state.selectedCompany?.id === id ? null : state.selectedCompany
      }))
    }),
    {
      name: 'company-store'
    }
  )
);