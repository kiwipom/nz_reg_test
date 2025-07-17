import React, { useState } from 'react';

const DateInputTest: React.FC = () => {
  const [testDate, setTestDate] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    console.log('Date input changed:', e.target.value);
    setTestDate(e.target.value);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    console.log('Key pressed:', e.key);
  };

  const handleInput = (e: React.FormEvent<HTMLInputElement>) => {
    console.log('Input event:', (e.target as HTMLInputElement).value);
  };

  return (
    <div className="p-8 max-w-md mx-auto">
      <h2 className="text-2xl font-bold mb-6">Date Input Test</h2>
      
      <div className="mb-6">
        <label htmlFor="test-date" className="block text-sm font-medium text-gray-700 mb-2">
          Test Date Input (Type a date like 2024-01-15)
        </label>
        <input
          type="date"
          id="test-date"
          value={testDate}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          onInput={handleInput}
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
          placeholder="Try typing a date"
        />
        <p className="mt-2 text-sm text-gray-600">Current value: {testDate || 'empty'}</p>
      </div>

      <div className="mb-6">
        <label htmlFor="text-date" className="block text-sm font-medium text-gray-700 mb-2">
          Text Input for Comparison
        </label>
        <input
          type="text"
          id="text-date"
          className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors"
          placeholder="Type anything here"
        />
      </div>

      <div className="text-sm text-gray-600">
        <p>Test Instructions:</p>
        <ol className="list-decimal list-inside mt-2 space-y-1">
          <li>Try typing directly in the date field above</li>
          <li>Check the browser console for logged events</li>
          <li>Compare with the text input behavior</li>
          <li>Try clicking the calendar icon if it appears</li>
        </ol>
      </div>
    </div>
  );
};

export default DateInputTest;