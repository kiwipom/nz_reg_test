import { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { DirectorService } from '../services/directorService';
import { useAuth0 } from '@auth0/auth0-react';
import DateInput from '../components/DateInput';

// Zod schema for director resignation form
const directorResignationSchema = z.object({
  directorId: z.number().min(1, 'Director ID is required'),
  resignationDate: z.string().min(1, 'Resignation date is required'),
  confirmResignation: z.boolean().refine(val => val === true, {
    message: 'You must confirm the resignation',
  }),
});

type DirectorResignationFormData = z.infer<typeof directorResignationSchema>;

interface DirectorResignationProps {
  directorId?: number;
  directorName?: string;
  onResignationComplete?: () => void;
}

export function DirectorResignation({ 
  directorId, 
  directorName, 
  onResignationComplete 
}: DirectorResignationProps) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionStatus, setSubmissionStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string>('');
  
  const { getAccessTokenSilently } = useAuth0();
  const directorService = new DirectorService();

  const {
    register,
    handleSubmit,
    formState: { errors },
    reset,
  } = useForm<DirectorResignationFormData>({
    resolver: zodResolver(directorResignationSchema),
    defaultValues: {
      directorId: directorId || 0,
      resignationDate: new Date().toISOString().split('T')[0], // Today's date
      confirmResignation: false,
    },
  });

  const onSubmit = async (data: DirectorResignationFormData) => {
    setIsSubmitting(true);
    setSubmissionStatus('idle');
    setErrorMessage('');

    try {
      const token = await getAccessTokenSilently();
      
      await directorService.resignDirector(
        data.directorId,
        data.resignationDate,
        token
      );

      setSubmissionStatus('success');
      reset();
      
      if (onResignationComplete) {
        onResignationComplete();
      }
    } catch (error) {
      console.error('Error resigning director:', error);
      setSubmissionStatus('error');
      setErrorMessage(error instanceof Error ? error.message : 'Failed to resign director');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-md">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-gray-900 mb-2">
          Director Resignation
        </h2>
        <p className="text-gray-600">
          Complete this form to resign a director from the company.
        </p>
        {directorName && (
          <p className="text-sm text-blue-600 mt-2">
            Resigning: <strong>{directorName}</strong>
          </p>
        )}
      </div>

      {submissionStatus === 'success' && (
        <div className="mb-6 p-4 bg-green-50 border border-green-200 rounded-md">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-green-800">
                Director resigned successfully
              </h3>
              <p className="text-sm text-green-700 mt-1">
                The director resignation has been processed and recorded.
              </p>
            </div>
          </div>
        </div>
      )}

      {submissionStatus === 'error' && (
        <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-md">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-red-800">
                Resignation failed
              </h3>
              <p className="text-sm text-red-700 mt-1">
                {errorMessage}
              </p>
            </div>
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
        <div>
          <label htmlFor="directorId" className="block text-sm font-medium text-gray-700 mb-1">
            Director ID
          </label>
          <input
            type="number"
            id="directorId"
            {...register('directorId', { valueAsNumber: true })}
            disabled={Boolean(directorId)} // Disable if pre-filled
            className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500 disabled:bg-gray-100"
          />
          {errors.directorId && (
            <p className="mt-1 text-sm text-red-600">{errors.directorId.message}</p>
          )}
        </div>

        <div>
          <Controller
            name="resignationDate"
            control={control}
            render={({ field }) => (
              <DateInput
                id="resignationDate"
                name="resignationDate"
                value={field.value || ''}
                onChange={field.onChange}
                label="Resignation Date"
                error={errors.resignationDate?.message}
              />
            )}
          />
        </div>

        <div className="flex items-start">
          <div className="flex items-center h-5">
            <input
              id="confirmResignation"
              type="checkbox"
              {...register('confirmResignation')}
              className="focus:ring-blue-500 h-4 w-4 text-blue-600 border-gray-300 rounded"
            />
          </div>
          <div className="ml-3 text-sm">
            <label htmlFor="confirmResignation" className="font-medium text-gray-700">
              Confirm resignation
            </label>
            <p className="text-gray-500">
              I confirm that this director is resigning from their position and that all 
              compliance requirements have been reviewed.
            </p>
          </div>
        </div>
        {errors.confirmResignation && (
          <p className="text-sm text-red-600">{errors.confirmResignation.message}</p>
        )}

        <div className="bg-yellow-50 border border-yellow-200 rounded-md p-4">
          <div className="flex">
            <div className="flex-shrink-0">
              <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
            </div>
            <div className="ml-3">
              <h3 className="text-sm font-medium text-yellow-800">
                Important Notice
              </h3>
              <div className="mt-2 text-sm text-yellow-700">
                <ul className="list-disc pl-5 space-y-1">
                  <li>Ensure the company will still meet the minimum director requirements</li>
                  <li>Verify that at least one NZ/Australian resident director will remain</li>
                  <li>This action cannot be undone - the director will need to be re-appointed if required</li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div className="flex justify-end space-x-3">
          <button
            type="button"
            onClick={() => reset()}
            className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
          >
            Reset
          </button>
          <button
            type="submit"
            disabled={isSubmitting}
            className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isSubmitting ? 'Resigning Director...' : 'Resign Director'}
          </button>
        </div>
      </form>
    </div>
  );
}