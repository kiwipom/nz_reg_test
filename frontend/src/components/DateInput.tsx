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
  const [inputValue, setInputValue] = useState(value);
  const [isCalendarOpen, setIsCalendarOpen] = useState(false);
  const textInputRef = useRef<HTMLInputElement>(null);
  const dateInputRef = useRef<HTMLInputElement>(null);

  // Convert between display format (DD/MM/YYYY) and ISO format (YYYY-MM-DD)
  const formatDateForDisplay = (isoDate: string): string => {
    if (!isoDate) return '';
    const [year, month, day] = isoDate.split('-');
    return `${day}/${month}/${year}`;
  };

  const formatDateForISO = (displayDate: string): string => {
    // Handle various input formats
    const cleaned = displayDate.replace(/[^\d]/g, '');
    
    if (cleaned.length === 8) {
      // DDMMYYYY
      const day = cleaned.slice(0, 2);
      const month = cleaned.slice(2, 4);
      const year = cleaned.slice(4, 8);
      
      // Validate date
      const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
      if (
        date.getFullYear() === parseInt(year) &&
        date.getMonth() === parseInt(month) - 1 &&
        date.getDate() === parseInt(day)
      ) {
        return `${year}-${month.padStart(2, '0')}-${day.padStart(2, '0')}`;
      }
    }
    
    return '';
  };

  const validateAndFormatInput = (input: string): string => {
    // Remove non-digit characters except slashes
    let cleaned = input.replace(/[^\d/]/g, '');
    
    // Auto-add slashes
    if (cleaned.length >= 2 && !cleaned.includes('/')) {
      cleaned = cleaned.slice(0, 2) + '/' + cleaned.slice(2);
    }
    if (cleaned.length >= 5 && cleaned.split('/').length === 2) {
      const parts = cleaned.split('/');
      cleaned = parts[0] + '/' + parts[1].slice(0, 2) + '/' + parts[1].slice(2);
    }
    
    // Limit length
    if (cleaned.length > 10) {
      cleaned = cleaned.slice(0, 10);
    }
    
    return cleaned;
  };

  useEffect(() => {
    setInputValue(formatDateForDisplay(value));
  }, [value]);

  const handleTextInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const rawValue = e.target.value;
    const formattedValue = validateAndFormatInput(rawValue);
    setInputValue(formattedValue);
    
    // Try to convert to ISO format and update parent
    const isoDate = formatDateForISO(formattedValue);
    if (isoDate || formattedValue === '') {
      onChange(isoDate);
    }
  };

  const handleTextInputBlur = () => {
    // Final validation on blur
    const isoDate = formatDateForISO(inputValue);
    if (isoDate) {
      setInputValue(formatDateForDisplay(isoDate));
      onChange(isoDate);
    } else if (inputValue && inputValue.length > 0) {
      // Invalid date, reset to empty
      setInputValue('');
      onChange('');
    }
  };

  const handleDateInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const isoDate = e.target.value;
    onChange(isoDate);
    setInputValue(formatDateForDisplay(isoDate));
    setIsCalendarOpen(false);
  };

  const toggleCalendar = () => {
    setIsCalendarOpen(!isCalendarOpen);
    if (!isCalendarOpen && dateInputRef.current) {
      // Focus the hidden date input to open native picker
      dateInputRef.current.focus();
      dateInputRef.current.click();
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
        {/* Text input for typing */}
        <input
          ref={textInputRef}
          type="text"
          id={id}
          name={name}
          value={inputValue}
          onChange={handleTextInputChange}
          onBlur={handleTextInputBlur}
          className={`${baseInputClasses} pr-12`}
          placeholder={placeholder}
          required={required}
          autoComplete="off"
        />
        
        {/* Hidden date input for calendar picker */}
        <input
          ref={dateInputRef}
          type="date"
          value={value}
          onChange={handleDateInputChange}
          className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
          tabIndex={-1}
          aria-hidden="true"
        />
        
        {/* Calendar icon button */}
        <button
          type="button"
          onClick={toggleCalendar}
          className="absolute inset-y-0 right-0 pr-3 flex items-center"
          aria-label="Open calendar"
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