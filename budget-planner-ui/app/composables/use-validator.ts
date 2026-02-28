export function useValidator() {
    function validate(value: 'string' | 'number', type: 'email' | 'password' | 'text') {
    }

    return {
        validate
    }
}