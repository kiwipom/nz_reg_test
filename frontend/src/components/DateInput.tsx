import React, { useState, useRef, useEffect } from 'react';
import { Calendar } from 'lucide-react';

interface DateInputProps {
  id?: string;
  name?: string;
  value: string;
  onChange: (value: string) => void;
  className?: string;
  required?: boolean;
  placeholder?: string;
  label?: string;
  error?: string;
}

const DateInput: React.FC<DateInputProps> = ({
  id,
  name,
  value,
  onChange,
  className = '',
  required = false,
  placeholder = 'DD/MM/YYYY',
  label,
  error
}) => {
  const [inputValue, setInputValue] = useState('');
  const [isFocused, setIsFocused] = useState(false);
  const textInputRef = useRef<HTMLInputElement>(null);
  const dateInputRef = useRef<HTMLInputElement>(null);

  // Convert between display format (DD/MM/YYYY) and ISO format (YYYY-MM-DD)
  const formatDateForDisplay = (isoDate: string): string => {
    if (!isoDate) return '';
    try {
      const [year, month, day] = isoDate.split('-');
      return `${day}/${month}/${year}`;
    } catch {
      return '';
    }
  };

  const parseDisplayDate = (displayDate: string): string => {
    if (!displayDate || displayDate.trim() === '') return '';
    
    // Remove any non-digit characters except slashes
    const cleaned = displayDate.replace(/[^\d/]/g, '');
    
    // Split by slash and handle different formats
    const parts = cleaned.split('/');
    
    if (parts.length === 3) {
      let day = parts[0].padStart(2, '0');
      let month = parts[1].padStart(2, '0');
      let year = parts[2];
      
      // Handle 2-digit years (assume 20xx for 00-30, 19xx for 31-99)
      if (year.length === 2) {
        const yearNum = parseInt(year);
        year = (yearNum <= 30 ? '20' : '19') + year;
      }
      
      // Validate the date
      if (year.length === 4) {
        const dayNum = parseInt(day);
        const monthNum = parseInt(month);
        const yearNum = parseInt(year);
        
        const date = new Date(yearNum, monthNum - 1, dayNum);
        if (
          date.getFullYear() === yearNum &&
          date.getMonth() === monthNum - 1 &&
          date.getDate() === dayNum &&
          dayNum >= 1 && dayNum <= 31 &&
          monthNum >= 1 && monthNum <= 12
        ) {
          return `${year}-${month}-${day}`;
        }
      }
    }
    
    return '';
  };

  const formatAsUserTypes = (input: string): string => {
    // Remove all non-digits
    const digitsOnly = input.replace(/\D/g, '');
    
    // Format as DD/MM/YYYY while typing
    let formatted = '';
    for (let i = 0; i < digitsOnly.length && i < 8; i++) {
      if (i === 2 || i === 4) {
        formatted += '/';
      }
      formatted += digitsOnly[i];
    }
    
    return formatted;
  };

  // Update display when value prop changes
  useEffect(() => {
    if (!isFocused) {
      setInputValue(formatDateForDisplay(value));
    }
  }, [value, isFocused]);

  const handleTextInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value;
    const formattedValue = formatAsUserTypes(rawValue);
    setInputValue(formattedValue);
  };

  const handleTextInputFocus = () => {
    setIsFocused(true);
  };

  const handleTextInputBlur = () => {
    setIsFocused(false);
    
    // Try to parse and validate the input
    const isoDate = parseDisplayDate(inputValue);
    if (isoDate) {
      onChange(isoDate);
      setInputValue(formatDateForDisplay(isoDate));
    } else if (inputValue.trim() !== '') {
      // Invalid date entered, clear it
      setInputValue('');
      onChange('');
    } else {
      // Empty input
      onChange('');
    }
  };

  const handleDateInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const isoDate = e.target.value;
    onChange(isoDate);
    setInputValue(formatDateForDisplay(isoDate));
  };

  const handleCalendarClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (dateInputRef.current) {
      dateInputRef.current.focus();
      dateInputRef.current.showPicker?.();
    }
  };

  const baseInputClasses = `w-full px-4 py-3 border rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors ${
    error ? 'border-red-500' : 'border-gray-300'
  } ${className}`;

  return (
    <div className="relative">
      {label && (
        <label htmlFor={id} className="block text-sm font-medium text-gray-700 mb-2">
          {label} {required && '*'}
        </label>
      )}
      
      <div className="relative">
        {/* Text input for typing - this is the main input */}
        <input
          ref={textInputRef}
          type="text"
          id={id}
          name={name}
          value={inputValue}
          onChange={handleTextInputChange}
          onFocus={handleTextInputFocus}
          onBlur={handleTextInputBlur}
          className={`${baseInputClasses} pr-12`}
          placeholder={placeholder}
          required={required}
          autoComplete="off"
          maxLength={10}
        />
        
        {/* Hidden date input for calendar picker - positioned below to avoid interference */}
        <input
          ref={dateInputRef}
          type="date"
          value={value}
          onChange={handleDateInputChange}
          className="absolute top-full left-0 w-0 h-0 opacity-0 pointer-events-none"
          tabIndex={-1}
          aria-hidden="true"
        />
        
        {/* Calendar icon button */}
        <button
          type="button"
          onClick={handleCalendarClick}
          className="absolute inset-y-0 right-0 pr-3 flex items-center hover:bg-gray-50 rounded-r-lg"
          aria-label="Open calendar"
          tabIndex={-1}
        >
          <Calendar className="h-5 w-5 text-gray-400 hover:text-gray-600" />
        </button>
      </div>
      
      {error && (
        <p className="mt-2 text-sm text-red-600">{error}</p>
      )}
      
      <p className="mt-1 text-xs text-gray-500">
        Type date as DD/MM/YYYY or click calendar icon
      </p>
    </div>
  );
};

export default DateInput;